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
package edu.uci.ics.sourcerer.tools.java.repo;

import edu.uci.ics.sourcerer.tools.java.repo.maven.MavenImporter;
import edu.uci.ics.sourcerer.tools.java.repo.model.JavaRepositoryFactory;
import edu.uci.ics.sourcerer.util.io.arguments.Arguments;
import edu.uci.ics.sourcerer.util.io.arguments.Command;

/**
 * @author Joel Ossher (jossher@uci.edu)
 */
public class Main {
  public static final Command AGGREGATE_JAR_FILES =
    new Command("aggregate-jar-files", "Collects all the project jar files into the jars directory.") {
      protected void action() {
        JavaRepositoryFactory.INSTANCE.loadModifiableJavaRepository(JavaRepositoryFactory.INPUT_REPO).aggregateJarFiles();
      }
    }.setProperties(JavaRepositoryFactory.INPUT_REPO);
    
  public static final Command IMPORT_MAVEN_TO_REPOSITORY =
    new Command("import-maven-to-repo", "Imports a copy of the Maven2 central repository into the Sourcerer repository.") {
      protected void action() {
        MavenImporter.importMavenToRepository();
      }
    }.setProperties(Arguments.INPUT, JavaRepositoryFactory.OUTPUT_REPO);
  
  public static void main(String[] args) {
    Command.execute(args, Main.class);
  }
}
