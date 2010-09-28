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
package edu.uci.ics.sourcerer.db.tools;

import static edu.uci.ics.sourcerer.util.io.Logging.logger;

import java.util.Collection;

import edu.uci.ics.sourcerer.db.util.DatabaseConnection;
import edu.uci.ics.sourcerer.model.db.LimitedProjectDB;
import edu.uci.ics.sourcerer.repo.extracted.Extracted;
import edu.uci.ics.sourcerer.util.Helper;
import edu.uci.ics.sourcerer.util.TimeCounter;

/**
 * @author Joel Ossher (jossher@uci.edu)
 */
public class ImportStageTwo extends ExtractedImporterThread {
  private Iterable<Extracted> extracted;
  
  protected ImportStageTwo(DatabaseConnection connection, SynchronizedUnknownsMap unknowns, Iterable<Extracted> extracted) {
    super(connection, unknowns);
    this.extracted = extracted;
  }

  @Override
  public void doImport() {
    TimeCounter counter = new TimeCounter();
    
    Collection<String> libraryProjects = projectsTable.getJavaLibraryProjects();
    libraryProjects.add(projectsTable.getPrimitiveProject());
    classifier = new RelationClassifier(libraryProjects);
    
    for (Extracted item : extracted) {
      logger.info("    Verifying that item should be imported...");
      if (!item.extracted()) {
        logger.info("      Extraction not completed... skipping");
        continue;
      } else if (!item.reallyExtracted()) {
        logger.info("      Extraction empty... skipping");
        continue;
      }
      LimitedProjectDB project;
      if (item.getHash() != null) {
        project = projectsTable.getLimitedProjectByHash(item.getHash());
      } else {
        project = projectsTable.getLimitedProjectByPath(item.getRelativePath());
      }
      if (project != null) {
        if (project.completed()) {
          logger.info("      Import already completed... skipping");
          continue;
        }
      }
      logger.info("Stage two import of " + item.getName() + "(" + item.getRelativePath() + ")");
      
      buildInClause(Helper.newHashSet(libraryProjects), item);
      String projectID = project.getProjectID();
      
      loadEntityMap(projectID);
      loadFileMap(projectID);
      
      if (item.getHash() != null) {
        projectsTable.beginSecondStageJarProjectInsert(projectID);
      } else {
        projectsTable.beginSecondStageCrawledProjectInsert(projectID);
      }
      
      insertRemainingEntities(item, projectID);
      loadRemainingEntityMap(projectID);
      insertRelations(item, projectID);
      insertImports(item, projectID);
      insertComments(item, projectID);
      
      if (item.getHash() != null) {
        projectsTable.completeJarProjectInsert(projectID);
      } else {
        projectsTable.completeCrawledProjectInsert(projectID);
      }
      
      clearMaps();
      counter.increment();
    }
    logger.info(counter.reportTimeAndCount(2, "items completed stage two of import"));
  }
}