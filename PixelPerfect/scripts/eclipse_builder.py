#!/usr/bin/python
#
# Copied from GoogleSearch eclipse_builder.py script.

"""Helps build eclipse projects."""

import fnmatch
import optparse
import os
import random
import shutil
import sys

FORMATTING_FILENAME = '.settings/org.eclipse.jdt.core.prefs'
TEMPLATES_FILENAME = '.settings/org.eclipse.jdt.ui.prefs'
PROJECT_PROPERTIES_FILENAME_R13_AND_LOWER = 'default.properties'
PROJECT_PROPERTIES_FILENAME_R14_AND_HIGHER = 'project.properties'


class Error(Exception):
  pass


def _GenerateId(seed):
  """Generates a random identifier, i.e., a string made of digits.

  Args:
    seed: a string, used as a seed for the randomly generated value

  Returns:
    a string, made of digits only
  """
  random.seed(seed)
  return str(random.randint(0, 1 << 32))


def MustExist(filename):
  """Makes sure that the give file exists.

  Args:
    filename: a string, the name of the file

  Raises:
    Error: if the file does not exist
  """
  if not os.path.exists(filename):
    raise Error('File not found: ' + filename)


def CreateDirsFor(pathname):
  """Recursively create the directory of the given file."""
  dir_path = os.path.dirname(pathname)
  if not os.path.exists(dir_path):
    os.makedirs(dir_path)
  if not os.path.isdir(dir_path):
    raise Error('Cannot create directory: %s' % dir_path)


def CopyToProject(project_root, project_name, src_filename, dst_filename):
  """Copies a file from the project root to the project directory.

  Args:
    project_root: a string, the location of the original project, not the
        Eclipse project
    project_name: a string, the name of the project where to create the file
    src_filename: a string, the name of the source file, relative to the project
        root
    dst_filename: a string, the name of the destination file, relative to the
        Eclipse project root
  """
  src = os.path.join(project_root, src_filename)
  MustExist(src)
  dst = os.path.join(project_name, dst_filename)
  CreateDirsFor(dst)
  shutil.copy(src, dst)


def EclipseCreate(name, contents=None):
  if not contents: contents = ''
  return ('Create', name, contents)


def EclipseLink(name, src=None, absolute=False):
  if not src:
    src = name
  return ('Link', src, name, absolute)


def EclipseLinkTree(name, src=None, glob=None):
  if not src:
    src = name
  return ('LinkTree', src, glob, name)


def EclipseLinkPlatform(platform_src, name, src):
  if platform_src:
    return EclipseLink(name, os.path.join(platform_src, src),
                       absolute=True)

def EclipseMkdir(name):
  return ('Mkdir', name)


def EclipseAndroidHome(path):
  return os.path.join(os.environ['ANDROID_HOME'], path)


def EclipseSrc(path, include=None, exclude=None):
  raw_args = [('including', include), ('excluding', exclude)]
  args = ' '.join([' %s="%s"' % (n, '|'.join(v)) for (n, v) in raw_args if v])
  return '<classpathentry kind="src" path="%s"%s/>' % (path, args)


def EclipseJREContainer(launcher=None):
  container = 'org.eclipse.jdt.launching.JRE_CONTAINER'
  if launcher is not None:
    container += '/' + launcher
  return '<classpathentry kind="con" path="%s"/>' % container


def EclipseJRE(version):
  return EclipseJREContainer(
      launcher=(
          'org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-%s'%
          version),
      )


def EclipseJUnit(version):
  return ('<classpathentry kind="con" '
          'path="org.eclipse.jdt.junit.JUNIT_CONTAINER/%s"/>' % version)


def EclipseAndroidFramework():
  return ('<classpathentry exported="true" kind="con" '
          'path="com.android.ide.eclipse.adt.ANDROID_FRAMEWORK"/>')

def EclipseAndroidPlatformSrc(platform_src, path):
  return EclipseSrc(path) if platform_src else None


def EclipseAndroidLibrary():
  return ('<classpathentry exported="true" kind="con" '
          'path="com.android.ide.eclipse.adt.LIBRARIES"/>')

def EclipseUserLibrary(userLibrary):
  return ('<classpathentry exported="true" kind="con" '
          'path="org.eclipse.jdt.USER_LIBRARY/%s"/>' % userLibrary)

def EclipseLib(path, sourcepath=None, exported=True):
  if sourcepath:
    sourcepathattr = ' sourcepath="%s"' % sourcepath
  else:
    sourcepathattr = ''
  return ('<classpathentry exported="%s" kind="lib" path="%s"%s/>' %
          (str(exported).lower(), path, sourcepathattr))


def EclipseProject(project):
  return ('<classpathentry combineaccessrules="false" kind="src" path="/%s"/>' %
          project)


def EclipseOutput(path):
  return '<classpathentry kind="output" path="%s"/>' % path


def EclipseAndroidProject(name, builders=None, linkedResources=None,
                          filtered_resources=None):
  if builders is None:
    builders = [
        EclipseAndroidResourceManager(),
        EclipseAndroidPreCompiler(),
        EclipseJavaBuilder(),
        EclipseAndroidApkBuilder(),
    ]
  builders = [builder for builder in builders if builder]

  if linkedResources:
    linkedResourcesNode = """\
  <linkedResources>
%s
  </linkedResources>""" % '\n'.join(linkedResources)
  else:
    linkedResourcesNode = ''

  if filtered_resources:
    filtered_resources_node = (
        '<filteredResources>%s</filteredResources>' %
        '\n'.join(filtered_resources))
  else:
    filtered_resources_node = ''

  return """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
  <name>%(name)s</name>
  <comment></comment>
  <projects>
  </projects>
  <buildSpec>
%(buildSpecChildren)s
  </buildSpec>
  <natures>
    <nature>com.android.ide.eclipse.adt.AndroidNature</nature>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
%(linkedResources)s
%(filtered_resources)s
</projectDescription>""" % {
    'name': name,
    'buildSpecChildren': '\n'.join(builders),
    'linkedResources': linkedResourcesNode,
    'filtered_resources': filtered_resources_node,
}


def EclipseJavaProject(name, builders):
  builders = [builder for builder in builders if builder]
  return """\
<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
  <name>%s</name>
  <comment></comment>
  <projects>
  </projects>
  <buildSpec>
%s
  </buildSpec>
  <natures>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
</projectDescription>""" % (name, '\n'.join(builders))


def EclipseAndroidResourceManager():
  return """\
    <buildCommand>
      <name>com.android.ide.eclipse.adt.ResourceManagerBuilder</name>
      <arguments>
      </arguments>
    </buildCommand>"""


def EclipseAndroidPreCompiler():
  return """\
    <buildCommand>
      <name>com.android.ide.eclipse.adt.PreCompilerBuilder</name>
      <arguments>
      </arguments>
    </buildCommand>"""


def EclipseJavaBuilder():
  return """\
    <buildCommand>
      <name>org.eclipse.jdt.core.javabuilder</name>
      <arguments>
      </arguments>
    </buildCommand>"""


def EclipseAndroidApkBuilder():
  return """\
    <buildCommand>
      <name>com.android.ide.eclipse.adt.ApkBuilder</name>
      <arguments>
      </arguments>
    </buildCommand>"""


def EclipseExternalToolBuilder(launcherName):
  if launcherName is None:
    raise Error('launcherName is required')
  return """\
    <buildCommand>
      <name>org.eclipse.ui.externaltools.ExternalToolBuilder</name>
      <triggers>clean,full,incremental,</triggers>
      <arguments>
        <dictionary>
          <key>LaunchConfigHandle</key>
          <value>&lt;project&gt;/.externalToolBuilders/%(launcherName)s.launch</value>
        </dictionary>
      </arguments>
    </buildCommand>""" % {
        'launcherName': launcherName,
    }


def EclipseLinkedResource(projectName, projectHash, projectType=2):
  if projectHash is None:
    # Eclipse generates 32-bit hashes using some unknown function. The
    # developer will have to manually link the resource in Eclipse, examine the
    # generated .project file, and copy it into their project template file.
    raise Error('projectHash is required')
  return """\
    <link>
      <name>%(projectName)s_src</name>
      <type>%(projectType)d</type>
      <locationURI>_android_%(projectName)s_%(projectHash)s/src</locationURI>
    </link>""" % {
        'projectName': projectName,
        'projectHash': projectHash,
        'projectType': projectType,
    }


def EclipseFilterNameMatches(value, name=''):
  return """<filter>
  <id>%(id)s</id>
  <name>%(name)s</name>
  <type>22</type>
  <matcher>
    <id>org.eclipse.ui.ide.multiFilter</id>
    <arguments>1.0-name-matches-true-false-%(value)s</arguments>
  </matcher>
</filter>""" % {
    'id': _GenerateId(name + value),
    'name': name,
    'value': value,
}


def EclipseFilterLocationMatches(value, name=''):
  return """<filter>
  <id>%(id)s</id>
  <name>%(name)s</name>
  <type>26</type>
  <matcher>
    <id>org.eclipse.ui.ide.multiFilter</id>
    <arguments>1.0-location-matches-false-false-%(value)s</arguments>
  </matcher>
</filter>""" % {
    'id': _GenerateId(name + value),
    'name': name,
    'value': value,
}


def EclipseAndroidActivityLauncher(name, project, activity=None, launch_in_background=True):
  if activity:
    # Specific activity
    activity_xml = '\
  <intAttribute key="com.android.ide.eclipse.adt.action" value="1"/> \
  <stringAttribute key="com.android.ide.eclipse.adt.activity" value="%s"/>' % \
      activity
  else:
    # Default activity
    activity_xml = """\
  <intAttribute key="com.android.ide.eclipse.adt.action" value="0"/>"""
  return (name, """\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="com.android.ide.eclipse.adt.debug.LaunchConfigType">
  %(activity_xml)s
  <stringAttribute key="com.android.ide.eclipse.adt.commandline" value=""/>
  <intAttribute key="com.android.ide.eclipse.adt.delay" value="0"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.nobootanim" value="false"/>
  <intAttribute key="com.android.ide.eclipse.adt.speed" value="0"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.target" value="true"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.wipedata" value="false"/>
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
    <listEntry value="/%(project)s"/>
    <listEntry value="/%(project)s/AndroidManifest.xml"/>
  </listAttribute>
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
    <listEntry value="4"/>
    <listEntry value="1"/>
  </listAttribute>
  <listAttribute key="org.eclipse.debug.ui.favoriteGroups">
    <listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
    <listEntry value="org.eclipse.debug.ui.launchGroup.run"/>
  </listAttribute>
  <booleanAttribute key="org.eclipse.jdt.launching.ALLOW_TERMINATE" value="true"/>
  <booleanAttribute key="org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND" value="%(launch_in_background)s"/>
  <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="%(project)s"/>
</launchConfiguration>""" % {
    'name': name,
    'project': project,
    'activity_xml': activity_xml,
    'launch_in_background': str(launch_in_background).lower(),
})


def EclipseAndroidTestLauncher(name, project, test=None, testsrc='src',
                               alltests=None,
                               instrumentationTestRunner=
                               'android.test.InstrumentationTestRunner'):
  """Builds the contents of an Eclipse JUnit Android Test Launcher file.

  Builds an Eclipse Android JUnit Launcher that launches either
  a specific Test/TestSuite specified by a fully-qualified name or
  all tests of the project.
  Either "test" or "alltests" must be provided.


  Args:
    name: A name of the launcher.
    project: A name of the Eclipse project.
    test: A fully-qualified name of the Test/TestSuite class to launch.
        This argument is ignored if the "alltests" argument is specified.
    testsrc: A root resource-path directory for "test" class source code.
        This argument is ignored if the "alltests" argument is specified.
    alltests: Specify to make the launcher run all tests in the project.
        If this argument is not specified, the "tests" argument is used.
    instrumentationTestRunner: A fully-qualified name of the Instrumentation
        test runner to use.

  Returns:
    A pair of strings, the name of the launcher and its contents.

  """
  if alltests is not None:
    resource_path = os.path.sep + project
    resource_path_type = 4
    container = "=" + project
    main_type = ""
  else:
    # Compute Eclipse resource path from test classname
    resource_path = os.path.sep + os.path.join(
        project, testsrc, test.replace('.', os.path.sep) + '.java')
    resource_path_type = 1
    container = ""
    main_type = test

  return (name, """\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="com.android.ide.eclipse.adt.junit.launchConfigurationType">
  <stringAttribute key="com.android.ide.eclipse.adt.commandline" value=""/>
  <intAttribute key="com.android.ide.eclipse.adt.delay" value="0"/>
  <stringAttribute key="com.android.ide.eclipse.adt.instrumentation" value="%(instrumentationTestRunner)s"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.nobootanim" value="false"/>
  <intAttribute key="com.android.ide.eclipse.adt.speed" value="0"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.target" value="true"/>
  <booleanAttribute key="com.android.ide.eclipse.adt.wipedata" value="false"/>
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
    <listEntry value="%(resource_path)s"/>
  </listAttribute>
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
    <listEntry value="%(resource_path_type)d"/>
  </listAttribute>
  <listAttribute key="org.eclipse.debug.ui.favoriteGroups">
    <listEntry value="org.eclipse.debug.ui.launchGroup.run"/>
    <listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
  </listAttribute>
  <stringAttribute key="org.eclipse.jdt.junit.CONTAINER" value="%(container)s"/>
  <stringAttribute key="org.eclipse.jdt.junit.TESTNAME" value=""/>
  <stringAttribute key="org.eclipse.jdt.junit.TEST_KIND" value="org.eclipse.jdt.junit.loader.junit3"/>
  <stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="%(main_type)s"/>
  <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="%(project)s"/>
</launchConfiguration>""" % {
    'project': project,
    'resource_path': resource_path,
    'resource_path_type': resource_path_type,
    'container': container,
    'main_type': main_type,
    'instrumentationTestRunner': instrumentationTestRunner,
})


def EclipseJavaTestLauncher(name, project, test=None, alltests=None):
  """Builds the contents of an Eclipse JUnit Launcher file.

  Builds an Eclipse JUnit Launcher that launches either
  a specific Test/TestSuite specified by a fully-qualified name or
  all tests in the specified directory.
  Either "test" or "alltests" must be provided.


  Args:
    name: A name of the launcher.
    project: A name of the Eclipse project.
    test: A fully-qualified name of the Test/TestSuite class to launch.
        This argument is ignored if the "alltests" argument is specified.
    alltests: A name of the directory containing all the tests to launch.
        If this argument is not specified, the "tests" argument is used.

  Returns:
    A pair of strings, the name of the launcher and its contents.

  """
  if alltests is not None:
    resourcePath = alltests
    container = "=" + project + "/" + alltests
    mainType = ""
  else:
    resourcePath = test.replace('.', '/')
    container = ""
    mainType = test

  return (name, """\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.jdt.junit.launchconfig">
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
    <listEntry value="/%s/%s"/>
  </listAttribute>
  <listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
    <listEntry value="1"/>
  </listAttribute>
  <booleanAttribute key="org.eclipse.debug.core.capture_output" value="false"/>
  <mapAttribute key="org.eclipse.debug.core.preferred_launchers">
    <mapEntry key="[run]" value="org.eclipse.jdt.junit.launchconfig"/>
  </mapAttribute>
  <booleanAttribute key="org.eclipse.debug.ui.ATTR_CONSOLE_OUTPUT_ON" value="false"/>
  <listAttribute key="org.eclipse.debug.ui.favoriteGroups">
    <listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
    <listEntry value="org.eclipse.debug.ui.launchGroup.run"/>
  </listAttribute>
  <stringAttribute key="org.eclipse.jdt.junit.CONTAINER" value="%s"/>
  <booleanAttribute key="org.eclipse.jdt.junit.KEEPRUNNING_ATTR" value="false"/>
  <stringAttribute key="org.eclipse.jdt.junit.TESTNAME" value=""/>
  <stringAttribute key="org.eclipse.jdt.junit.TEST_KIND" value="org.eclipse.jdt.junit.loader.junit4"/>
  <stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="%s"/>
  <stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="%s"/>
</launchConfiguration>""" % (project,
                             resourcePath, container, mainType, project))


def EclipseAntLauncher(name, project, build_xml='build.xml', args='',
                       working_dir='${workspace_loc}'):
  """Builds the contents of an Eclipse Ant Launcher file.

  Builds a launcher that launches an Ant build with the given parameters.

  Args:
    name: A name of the launcher.
    project: A name of the Eclipse project.
    build_xml: Optional build file name. If not provided, build.xml
               is used as the default
    args: Optional arguments. Default is no args
    working_dir: Optional name of the directory to launch the ant
                build from. If not specified, this is the workspace
                location.

  Returns:
    A pair of strings, the name of the launcher and its contents.

  """

  build_path = '/%s/%s' % (project, build_xml)

  return (name, """\
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.ant.AntLaunchConfigurationType">
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
<listEntry value="%s"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
<listEntry value="1"/>
</listAttribute>
<stringAttribute key="org.eclipse.jdt.launching.CLASSPATH_PROVIDER" value="org.eclipse.ant.ui.AntClasspathProvider"/>
<stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="org.eclipse.ant.internal.launching.remote.InternalAntRunner"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="%s"/>
<stringAttribute key="org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER" value="org.eclipse.ant.ui.AntClasspathProvider"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_LOCATION" value="${workspace_loc:%s}"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_TOOL_ARGUMENTS" value="%s"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY" value="%s"/>
<stringAttribute key="process_factory_id" value="org.eclipse.ant.ui.remoteAntProcessFactory"/>
</launchConfiguration>""" % (build_path,
                             project, build_path, args, working_dir))


def EclipseGoogle3AndroidApp(project_file, project_root,
                             package, name=None, include=None,
                             exclude=None, java_root=None,
                             third_party_libs=None, root=None,
                             third_party_link_name=None,
                             resources='res',
                             manifest='AndroidManifest.xml'):
  if not name:
    name = package.split('.')[-1]
  package_path = package.replace('.', os.path.sep)
  if not java_root:
    java_root = (('../' * len(package_path.split(os.path.sep))) +
                 '../java')
  if not root:
    root = os.path.join(java_root, '..')

  if not third_party_link_name:
    third_party_link_name = 'libs'

  files = [
      EclipseLink(GetProjectPropertiesFilename(project_root)),
      EclipseLink('src', src='%s' % java_root),
      EclipseLink('AndroidManifest.xml', src='%s' % manifest),
      EclipseLink('res', src='%s' % resources),
      EclipseLink(os.path.basename(project_file), src=project_file),
      EclipseMkdir('gen'),
  ]

  classpath = [
      EclipseSrc('src',
                 include=['%s/**' % package_path] + (include or []),
                 exclude=['%s/res/**' % package_path] + (exclude or [])
                ),
      EclipseSrc('gen'),
      EclipseAndroidFramework(),
      EclipseOutput('bin'),
  ]

  _AddThirdPartyLibs(files, classpath, third_party_libs, third_party_link_name,
                     root)

  project = EclipseAndroidProject(name=name)

  return (name, files, classpath, project)


def EclipseGoogle3AndroidTests(project_file, project_root,
                               package, name=None,
                               include=None, exclude=None, javatests_root=None,
                               third_party_libs=None, root=None,
                               third_party_link_name=None, resources='res',
                               manifest='AndroidManifest.xml'):
  if not name:
    name = package.split('.')[-1]
  package_path = package.replace('.', os.path.sep)
  if not javatests_root:
    javatests_root = (('../' * len(package_path.split(os.path.sep))) +
                      '../javatests')
  if not root:
    root = os.path.join(javatests_root, '..')

  if not third_party_link_name:
    third_party_link_name = 'libs'

  files = [
      EclipseLink(GetProjectPropertiesFilename(project_root)),
      EclipseLink('src', src='%s' % javatests_root),
      EclipseLink('AndroidManifest.xml', src='%s' % manifest),
      EclipseLink('res', src='%s' % resources),
      EclipseLink(os.path.basename(project_file), src=project_file),
      EclipseMkdir('gen'),
  ]

  classpath = [
      EclipseSrc('src',
                 include=['%s/**' % package_path] + (include or []),
                 exclude=['%s/res/**' % package_path] + (exclude or [])
                ),
      EclipseSrc('gen'),
      EclipseAndroidFramework(),
      EclipseProject(name),
      EclipseOutput('bin'),
  ]

  _AddThirdPartyLibs(files, classpath, third_party_libs, third_party_link_name,
                     root)

  project = EclipseAndroidProject(name='%s-tests' % name)

  return ('%s-tests' % name, files, classpath, project)


def GetProjectPropertiesFilename(project_root):
  """Gets the name of the Android project .properties file.

  This methods chooses project.properties (if found) and falls back
  to default.properties.

  Args:
    project_root: The root directory of the project.

  Returns:
    The name of the properties file.

  """
  result = PROJECT_PROPERTIES_FILENAME_R14_AND_HIGHER
  if not os.path.exists(os.path.join(project_root, result)):
    result = PROJECT_PROPERTIES_FILENAME_R13_AND_LOWER
  return result


def _AddThirdPartyLibs(files, classpath, third_party_libs, link_name, root):
  for third_party_lib in third_party_libs or []:
    lib_name = os.path.join(link_name, os.path.basename(third_party_lib))
    lib_orig = os.path.join(root, 'third_party', 'java', third_party_lib)
    files.append(EclipseLink(lib_name, src=lib_orig))
    third_party_src_lib = third_party_lib.replace('.jar', '-src.jar')
    src_lib_name = os.path.join(link_name,
                                os.path.basename(third_party_src_lib))
    src_lib_orig = os.path.join(root, 'third_party', 'java',
                                third_party_src_lib)
    files.append(EclipseLink(src_lib_name, src=src_lib_orig))
    classpath.append(EclipseLib(lib_name, sourcepath=src_lib_name))


def RelativePath(path, pwd):
  path = os.path.normpath(os.path.abspath(path))
  path = path.split(os.path.sep)
  pwd = os.path.normpath(os.path.abspath(pwd)).split(os.path.sep)
  while path[0] == pwd[0]:
    path.pop(0)
    pwd.pop(0)
  while pwd:
    path.insert(0, '..')
    pwd.pop(0)
  return os.path.join(*path)


def AppendNameSuffix(name, suffix):
  if suffix:
    return '%s-%s' % (name, suffix)
  else:
    return name


def CreateProject(args):
  parser = optparse.OptionParser()
  parser.add_option('--project_file', help='The name of the project file')
  parser.add_option('--project_root',
                    help='The root directory of the project')
  parser.add_option('--project_name_suffix',
                    help='A suffix to append to the generated project name')
  parser.add_option('--platform_src',
                    help='The location of the android source tree')
  options, args = parser.parse_args(args)
  if args:
    raise Error('Unexpected argument: %s' % args[0])
  if not options.project_file:
    raise Error('Missing --project_file')

  if not os.path.exists(options.project_file):
    raise Error('File not found: %s' % options.project_file)

  if not options.project_root:
    project_root = os.path.dirname(os.path.abspath(options.project_file))
  else:
    project_root = options.project_root
  if not os.path.exists(project_root):
    raise Error('Directory not found: %s' % project_root)

  if not options.project_name_suffix:
    project_name_suffix = None
  else:
    project_name_suffix = options.project_name_suffix

  # add the project file argument to the method calls that need it.
  wrap = (lambda x :
          lambda *args, **kwargs : x(options.project_file, project_root,
                                     *args, **kwargs))
  name_suffix_wrap = (lambda x :
                      lambda name, *args, **kwargs : x(AppendNameSuffix(
                          name, project_name_suffix), *args, **kwargs))

  name_keyword_suffix_wrap = (
      lambda x :
      lambda *args, **kwargs : x(
          *args, name=AppendNameSuffix(kwargs.pop('name', None),
                                       project_name_suffix),
          **kwargs))

  append_suffix = (lambda name: AppendNameSuffix(name, project_name_suffix))

  wrap_pass_platform_src = (lambda x :
                            lambda *args, **kwargs : x(options.platform_src,
                                                        *args, **kwargs))

  functions = {
      'Create': EclipseCreate,
      'Link': EclipseLink,
      'LinkFramework': wrap_pass_platform_src(EclipseLinkPlatform),
      'LinkTree': EclipseLinkTree,
      'Mkdir': EclipseMkdir,
      'AndroidHome': EclipseAndroidHome,
      'Src': EclipseSrc,
      'JREContainer': EclipseJREContainer,
      'JRE': EclipseJRE,
      'JUnit': EclipseJUnit,
      'AndroidFramework': EclipseAndroidFramework,
      'AndroidFrameworkSrc': wrap_pass_platform_src(
          EclipseAndroidPlatformSrc),
      'AndroidLibrary': EclipseAndroidLibrary,
      'Lib': EclipseLib,
      'UserLibrary' : EclipseUserLibrary,
      'Project': name_suffix_wrap(EclipseProject),
      'Output': EclipseOutput,
      'AndroidProject': name_suffix_wrap(EclipseAndroidProject),
      'JavaProject': name_suffix_wrap(EclipseJavaProject),
      'AndroidResourceManager': EclipseAndroidResourceManager,
      'AndroidPreCompiler': EclipseAndroidPreCompiler,
      'JavaBuilder': EclipseJavaBuilder,
      'AndroidApkBuilder': EclipseAndroidApkBuilder,
      'ExternalToolBuilder': EclipseExternalToolBuilder,
      'LinkedResource': EclipseLinkedResource,
      'FilterNameMatches': EclipseFilterNameMatches,
      'FilterLocationMatches': EclipseFilterLocationMatches,
      'AndroidActivityLauncher': EclipseAndroidActivityLauncher,
      'AndroidTestLauncher': EclipseAndroidTestLauncher,
      'JavaTestLauncher': EclipseJavaTestLauncher,
      'AntLauncher': EclipseAntLauncher,
      'Google3AndroidApp': name_keyword_suffix_wrap(
          wrap(EclipseGoogle3AndroidApp)),
      'Google3AndroidTests': name_keyword_suffix_wrap(
          wrap(EclipseGoogle3AndroidTests)),
      'AppendSuffix': append_suffix,
      'source_root': project_root,
  }
  results = {}
  exec(file(options.project_file)).read() in functions, results
  HandleResults(project_root, results, project_name_suffix=project_name_suffix)


def HandleResults(project_root, results, project_name_suffix=None):
  """Handles the data obtained by reading the project configuration.

  Args:
    project_root: a string, the root of the original project, not the Eclipse
        project
    results: a directory, the variables defined in the project file.
    project_name_suffix: a suffix to be appended to the generated projects name

  Raises:
    Error: is anything goes wrong in the generation of the Eclipse project
  """
  if 'name' in results:
    project_name = AppendNameSuffix(results['name'].replace(os.path.sep, ' '),
                                    project_name_suffix)
  else:
    raise Error('Missing project name')

  if 'parent_path' in results:
    project_name = os.path.join(results['parent_path'], project_name)

  if not os.path.exists(project_name):
    os.makedirs(project_name)

  if 'files' in results:
    for entry in results['files']:
      if entry is None:
        continue
      if entry[0] == 'Link':
        (_, link_src, link_dst, absolute) = entry
        if absolute:
          src = link_src
        else:
          src = os.path.join(project_root, link_src)
        if not os.path.exists(src):
          raise Error('Linked file not found: %s' % src)
      elif entry[0] == 'LinkTree':
        (_, link_src, link_glob, link_dst) = entry
        src = os.path.join(project_root, link_src)
        if not os.path.exists(src):
          raise Error('Linked tree not found: %s' % src)
        if not os.path.isdir(src):
          raise Error('Linked tree is not a directory: %s' % src)
      elif entry[0] == 'Mkdir':
        # Just validate that the entry is properly formatted.
        (_, dir_name) = entry
      elif entry[0] == 'Create':
        # Just validate that the entry is properly formatted.
        (_, name, contents) = entry
      else:
        raise Error('Invalid files definition')

    for entry in results['files']:
      if entry is None:
        continue
      if entry[0] == 'Link':
        (_, link_src, link_dst, absolute) = entry
        dst = os.path.join(project_name, link_dst)
        link_dir = os.path.dirname(dst)
        if absolute:
          src = link_src
        else:
          src = RelativePath(os.path.join(project_root, link_src), link_dir)
        if os.path.exists(dst):
          os.unlink(dst)
        if os.path.islink(dst):
          os.unlink(dst)

        if not os.path.exists(link_dir):
          os.makedirs(link_dir)
        try:
          os.symlink(src, dst)
        except Exception:
          raise Error('os.symlink(%s, %s) failed' % (repr(src), repr(dst)))
        if not os.path.exists(dst):
          raise Error('Cannot create symlink: %s' % dst)
        if not os.path.islink(dst):
          raise Error('Cannot create symlink: %s' % dst)
      elif entry[0] == 'LinkTree':
        (_, link_src, link_glob, link_dst) = entry
        root_src = os.path.join(project_root, link_src)
        if not root_src.endswith('/'):
          root_src += '/'
        root_dst = os.path.join(project_name, link_dst)
        if os.path.islink(root_dst):
          os.unlink(root_dst)

        if not os.path.exists(root_dst):
          os.makedirs(root_dst)

        for (path, _, files) in os.walk(root_src):
          assert path.startswith(root_src)
          path = path[len(root_src):]
          for f in files:
            src = os.path.join(root_src, path, f)
            dst = os.path.join(root_dst, path, f)
            link_dir = os.path.dirname(dst)
            src = RelativePath(src, link_dir)
            if os.path.exists(dst):
              os.unlink(dst)
            if not os.path.exists(link_dir):
              os.makedirs(link_dir)
            if not os.path.isdir(src):
              try:
                os.symlink(src, dst)
              except Exception:
                raise Error('os.symlink(%s, %s) failed' % (repr(src),
                                                           repr(dst)))
              if not os.path.exists(dst):
                raise Error('Cannot create symlink: %s' % dst)
              if not os.path.islink(dst):
                raise Error('Cannot create symlink: %s' % dst)
      elif entry[0] == 'Mkdir':
        (_, dir_name) = entry
        dir_path = os.path.join(project_name, dir_name)
        if not os.path.exists(dir_path):
          os.makedirs(dir_path)
        if not os.path.isdir(dir_path):
          raise Error('Cannot create directory: %s' % dir_name)
      elif entry[0] == 'Create':
        (_, name, contents) = entry
        name_path = os.path.join(project_name, name)
        f = open(name_path, 'w')
        f.write(contents)
        f.close()
        if not os.path.isfile(name_path):
          raise Error('Cannot create file: %s' % name)
      else:
        raise Error('Invalid files definition')

  if 'classpath' in results:
    fp = open(os.path.join(project_name, '.classpath'), 'w')
    fp.write('<?xml version="1.0" encoding="UTF-8"?>\n')
    fp.write('<classpath>\n')
    for entry in results['classpath']:
      if entry:
        fp.write('  %s\n' % entry)
    fp.write('</classpath>\n')
    fp.close()

  if 'project' in results:
    fp = open(os.path.join(project_name, '.project'), 'w')
    fp.write(results['project'])
    fp.close()

  if 'launchers' in results:
    for launcher in results['launchers']:
      (name, contents) = launcher
      fp = open(os.path.join(project_name,
                             '%s.launch' % name.replace(os.path.sep, ' ')),
                'w')
      fp.write(contents)
      fp.close()

  if 'formatting' in results:
    CopyToProject(project_root, project_name, results['formatting'],
                  FORMATTING_FILENAME)

  if 'templates' in results:
    CopyToProject(project_root, project_name, results['templates'],
                  TEMPLATES_FILENAME)


def main(args):
  try:
    CreateProject(args)
  except Error as e:
    sys.stderr.write('ERROR: %s\n' % e.args[0])
    return 1


if __name__ == '__main__':
  sys.exit(main(sys.argv[1:]))
