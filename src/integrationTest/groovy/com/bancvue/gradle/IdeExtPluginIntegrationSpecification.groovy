/**
 * Copyright 2013 BancVue, LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bancvue.gradle

import com.bancvue.gradle.test.AbstractPluginIntegrationSpecification

import static org.junit.Assert.fail

class IdeExtPluginIntegrationSpecification extends AbstractPluginIntegrationSpecification {

	def "idea should add standard source directories and additional test configurations if ide plugin declared before test plugin"() {
		given:
		buildFile << """
apply plugin: 'groovy'
apply plugin: 'com.bancvue.ide-ext'
apply plugin: 'com.bancvue.integration-test'

repositories {
	mavenCentral()
}

dependencies {
	integrationTestCompile 'org.apache.commons:commons-collections4:4.0'
}

sourceSets {
	other
}
        """
		mkdir("src/main/java")
		mkdir("src/other/java")
		mkdir("src/test/groovy")
		mkdir("src/integrationTest/groovy")

		when:
		run("idea")

		then:
		File expectedImlFile = file("${projectFS.name}.iml")
		expectedImlFile.exists()
		assertIdeaModuleFileContainsExpectedDependency(expectedImlFile, "/org.apache.commons/commons-collections4/4.0/")
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, "src/main/java", false)
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, "src/other/java", false)
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, "src/test/groovy", true)
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, "src/integrationTest/groovy", true)
		try {
			assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, "src/someOtherTest/groovy", true)
			fail("Test should have failed, something is likely wrong with positive validations")
		} catch (Throwable ex) {}
	}

	private void assertIdeaModuleFileContainsExpectedResourceFolder(File expectedImlFile, String folderName, boolean isTestFolder) {
		assertIdeaModuleFileContainsExpectedSourceFolder(expectedImlFile, folderName, isTestFolder, true)
	}

	private void assertIdeaModuleFileContainsExpectedSourceFolder(File expectedImlFile, String folderName, boolean isTestFolder, boolean isResourceFolder = false) {
		String expectedUrl = "file://\$MODULE_DIR\$/${folderName}"
		def module = new XmlParser().parseText(expectedImlFile.text)

		List result = module.component.content.sourceFolder.findAll {
			it.@url == expectedUrl
		}

		if (!result) {
			fail("Expected sourceFolder url=${expectedUrl} not found in iml content=${expectedImlFile.text}")
		}
		assert result.size() == 1
		assert Boolean.parseBoolean(result[0].@isTestSource) == isTestFolder
		if (isResourceFolder) {
			assert result[0].@type == (isTestFolder ? "java-test-resource" : "java-resource")
		} else {
			assert result[0].@type == null
		}
	}

	private void assertIdeaModuleFileContainsExpectedDependency(File expectedImlFile, String expectedUrlPath) {
		def module = new XmlParser().parseText(expectedImlFile.text)

		List result = module.component.orderEntry.library.CLASSES.root.findAll {
			it.@url =~ expectedUrlPath
		}

		if (!result) {
			fail("Expected dependency url=${expectedUrlPath} not found in iml content=${expectedImlFile.text}")
		}
	}

	def "eclipse should add standard source directories and additional test configurations if ide plugin declared after test plugin"() {
		given:
		buildFile << """
apply plugin: 'groovy'
apply plugin: 'com.bancvue.ide-ext'
apply plugin: 'com.bancvue.component-test'

repositories {
	mavenCentral()
}

dependencies {
	componentTestCompile 'org.apache.commons:commons-collections4:4.0'
}

        """
		mkdir("src/main/java")
		mkdir("src/test/groovy")
		mkdir("src/componentTest/groovy")

		when:
		run("eclipse")

		then:
		File expectedClasspathFile = file(".classpath")
		assert expectedClasspathFile.exists()
		assertEclipseModuleFileContainsExpectedDependency(expectedClasspathFile, "/org.apache.commons/commons-collections4/4.0/")
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, "src/main/java")
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, "src/test/groovy")
		assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, "src/componentTest/groovy")
		try {
			assertEclipseModuleFileContainsExpectedSourceFolder(expectedClasspathFile, "src/someOtherTest/groovy")
			fail("Test should have failed, something is likely wrong with positive validations")
		} catch (Throwable ex) {}
	}

	private void assertEclipseModuleFileContainsExpectedSourceFolder(File expectedClasspathFile, String folderName) {
		assertEclipseModuleFileContainsExpectedPath(expectedClasspathFile, "src", /^${folderName}$/)
	}

	private void assertEclipseModuleFileContainsExpectedDependency(File expectedClasspathFile, String dependencyPath) {
		assertEclipseModuleFileContainsExpectedPath(expectedClasspathFile, "lib", dependencyPath)
	}

	private void assertEclipseModuleFileContainsExpectedPath(File expectedClasspathFile, String kind, String dependencyPath) {
		def classpath = new XmlParser().parseText(expectedClasspathFile.text)
		List result = classpath.classpathentry.findAll {
			it.@kind == kind && it.@path =~ dependencyPath
		}

		if (!result) {
			fail("Expected ${kind} classpathentry path=${dependencyPath} not found in .classpath content=${expectedClasspathFile.text}")
		}
	}

	def "idea should augment resource directories with type"() {
		given:
		buildFile << """
apply plugin: 'groovy'
apply plugin: 'com.bancvue.ide-ext'
apply plugin: 'com.bancvue.integration-test'
        """
		mkdir("src/main/resources")
		mkdir("src/test/resources")
		mkdir("src/integrationTest/resources")

		when:
		run("idea")

		then:
		File expectedImlFile = file("${projectFS.name}.iml")
		expectedImlFile.exists()
		assertIdeaModuleFileContainsExpectedResourceFolder(expectedImlFile, "src/main/resources", false)
		assertIdeaModuleFileContainsExpectedResourceFolder(expectedImlFile, "src/test/resources", true)
		assertIdeaModuleFileContainsExpectedResourceFolder(expectedImlFile, "src/integrationTest/resources", true)
	}

}