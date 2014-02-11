#!/usr/bin/python
#
# Generates the eclipse projects for PixelPerfect.
#
# Adapter from GoogleSearch eclipse_setup.py script.

import optparse
import os
import sys


def check_path(path, message, create=False):
  """Returns the full path to an exisiting file, resolving symlinks or exits."""
  realpath = os.path.realpath(path)
  if not os.path.exists(realpath):
    if create:
      os.mkdir(realpath)
    else:
      sys.exit("%s did not exist, tried %s" % (message, path))
  return realpath

def list_callback(option, opt, value, parser):
  setattr(parser.values, option.dest, value.split(','))

def main(args):
  parser = optparse.OptionParser()
  parser.set_defaults(projects=('pixelperfect-platform','pixelperfect-platform-test'))
  parser.add_option('--platform_src',
                    help='Location of the android platform source tree')

  options, args = parser.parse_args(args)
  script_path = os.path.realpath(sys.argv[0])
  scripts_dir = os.path.dirname(script_path)
  par_file = check_path("%s/eclipse_builder.py" % scripts_dir,
      "eclipse_builder")
  project_files_dir = check_path("%s/../eclipse" % scripts_dir,
      "project files folder")
  android_root = check_path("%s/../../../.." % scripts_dir, "android root")
  # Put eclipse files in home dir on OS X, and in a sibling dir on Linux.
  # This is because on OS X the source tree is typically in a mounted disk image
  if sys.platform == 'darwin':
    output_dir = check_path(os.path.expanduser("~/eclipse"), "eclipse folder",
        True)
  else:
    output_dir = check_path("%s/../eclipse" % android_root, "eclipse folder",
        True)
  output_dir = check_path(("%s/%s") % (output_dir, "pixelperfect/platform"),
      "project subdirectory", True)
  project_root = check_path("%s/.." % scripts_dir, "project root")
  print "project_files_dir: %s" % project_files_dir
  print "android_root: %s" % android_root
  print "eclipse_folder: %s" % output_dir
  print "project_root: %s" % project_root

  def is_allowed_project_file(filename):
    if not filename.endswith('.project'):
      return False
    return os.path.splitext(os.path.basename(filename))[0] in options.projects

  for file in sorted(filter(is_allowed_project_file, os.listdir(project_files_dir))):
    project_folder = "%s/%s" % (output_dir, file.replace(".project", ""))
    if os.path.exists(project_folder):
      os.system("rm -r %s" % project_folder)
    project_file = check_path("%s/%s" % (project_files_dir, file),
        "project file")
    command = ("cd %s && %s --project_file=%s --project_root=%s" %
               (output_dir, par_file, project_file, project_root))
    if options.platform_src:
      command = ("%s --platform_src=%s" % (command, options.platform_src))
    print "processing '%s'" % os.path.basename(project_file)
    os.system(command)

if __name__ == '__main__':
  sys.exit(main(sys.argv[1:]))
