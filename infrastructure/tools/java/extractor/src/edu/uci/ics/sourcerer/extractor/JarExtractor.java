/* 
 * Sourcerer: an infrastructure for large-scale source code analysis.
 * Copyright (C) by contributors. See CONTRIBUTORS.txt for full list.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package edu.uci.ics.sourcerer.extractor;

import static edu.uci.ics.sourcerer.repo.general.AbstractRepository.INPUT_REPO;
import static edu.uci.ics.sourcerer.repo.general.AbstractRepository.OUTPUT_REPO;
import static edu.uci.ics.sourcerer.util.io.Logging.logger;

import java.util.Collection;

import org.eclipse.jdt.core.IClassFile;

import edu.uci.ics.sourcerer.extractor.ast.FeatureExtractor;
import edu.uci.ics.sourcerer.extractor.ast.FeatureExtractor.ClassExtractionReport;
import edu.uci.ics.sourcerer.extractor.io.WriterBundle;
import edu.uci.ics.sourcerer.extractor.resolver.MissingTypeResolver;
import edu.uci.ics.sourcerer.extractor.resources.EclipseUtils;
import edu.uci.ics.sourcerer.repo.base.Repository;
import edu.uci.ics.sourcerer.repo.extracted.ExtractedJar;
import edu.uci.ics.sourcerer.repo.extracted.ExtractedRepository;
import edu.uci.ics.sourcerer.repo.general.IndexedJar;
import edu.uci.ics.sourcerer.repo.general.JarIndex;
import edu.uci.ics.sourcerer.util.io.FileUtils;
import edu.uci.ics.sourcerer.util.io.Logging;

/**
 * @author Joel Ossher (jossher@uci.edu)
 */
public class JarExtractor {
  public static void extract(MissingTypeResolver resolver) {
    // Load the input repository
    logger.info("Loading the input repository...");
    Repository input = Repository.getRepository(INPUT_REPO.getValue());
    
    // Load the output repository
    ExtractedRepository output = ExtractedRepository.getRepository(OUTPUT_REPO.getValue());
    
    logger.info("Getting the jar index...");
    JarIndex index = input.getJarIndex();
    logger.info("--- Extracting " + index.getIndexSize() + " jars ---");
    int count = 0;
    for (IndexedJar jar : index.getIndexedJars()) {
      logger.info("Extracting " + jar.toString() + " (" + ++count + " of " + index.getIndexSize() + ")");
      ExtractedJar extracted = jar.getExtractedJar(output);
      if (Extractor.EXTRACT_BINARY.getValue() && extracted.extracted()) {
        logger.info("  Jar already extracted");
      } else if (Extractor.RESOLVE_MISSING_TYPES.getValue() && extracted.extracted() && !extracted.sourceSkipped()) {
        logger.info("  Jar already extracted");
      } else if (extracted.hasMissingTypes()) {
        logger.info("  Jar has missing types");
      } else if (extracted.extracted() && !extracted.sourceSkipped()) {
        logger.info("  Jar already extracted");
      } else {
        // Set up logging
        Logging.addFileLogger(extracted.getContent());

        Collection<IndexedJar> previousJars = null;
        Collection<IndexedJar> jars = null;
        boolean missingTypes = extracted.hasMissingTypes();
        while (true) {
          // Set up the writer bundle
          WriterBundle bundle = new WriterBundle(extracted.getContent());

          if (missingTypes) {
            logger.info("  Resolving missing types...");
            jars = resolver.resolveMissingTypes(index, extracted, bundle.getUsedJarWriter());
            jars.add(jar);
            logger.info("  Initializing project with " + jars.size() + " jars...");
            EclipseUtils.initializeJarProject(jars, index);
          } else {
            logger.info("  Initializing project...");
            EclipseUtils.initializeJarProject(jar, index);
          }
          
          logger.info("  Getting class files...");
          Collection<IClassFile> classFiles = EclipseUtils.getClassFiles(jar);
          
          logger.info("  Extracting " + classFiles.size() + " class files...");
                  
          // Set up the feature extractor
          FeatureExtractor extractor = new FeatureExtractor(bundle);

          boolean force = previousJars != null && jars.size() <= previousJars.size(); 

          // Extract
          ClassExtractionReport report = extractor.extractClassFiles(classFiles, force);
          
          // Close the output files
          extractor.close();
          
          previousJars = jars;
          
          if (Extractor.EXTRACT_BINARY.getValue()) {
            extracted.reportBinaryExtraction(report.getExtractedFromBinary(), report.getBinaryExtractionExceptions(), report.sourceSkipped());
            break;
          } else if (Extractor.RESOLVE_MISSING_TYPES.getValue()) {
            if (force) {
              extracted.reportForcedExtraction(report.getExtractedFromBinary(), report.getBinaryExtractionExceptions(), report.getExtractedFromSource(), report.getSourceExtractionExceptions());
              break;
            } else if (!(report.hadMissingType() || report.hadMissingSecondOrder())) {
              extracted.reportSuccessfulExtraction(report.getExtractedFromBinary(), report.getBinaryExtractionExceptions(), report.getExtractedFromSource(), report.getSourceExtractionExceptions());
              break;
            } else {
              missingTypes = report.hadMissingType() || report.hadMissingSecondOrder();
              logger.info("  Redoing because of missing types...");
            }
          } else {
            // Write the properties file
            if (report.hadMissingType() || report.hadMissingSecondOrder()) {
              extracted.reportMissingTypeExtraction();
            } else {
              extracted.reportSuccessfulExtraction(report.getExtractedFromBinary(), report.getBinaryExtractionExceptions(), report.getExtractedFromSource(), report.getSourceExtractionExceptions());
            }
            break;
          }
        }
       
        // End the error logging
        Logging.removeFileLogger(extracted.getContent());
      }
    }
    
    logger.info("Copying jar index file...");
    FileUtils.copyFile(input.getJarIndexFile(), output.getJarIndexFile());
    
    logger.info("Done!");
  }
}
