#!/usr/bin/python
#
# Imports protos for PixelPerfect and fixes them to work with Android's
# proto compiler.
#
# Adapted from google3/wireless/voicesearch/tools/update_sidekick_proto.py

import os
import re
import sys

base_google3_path = "/home/build/google3/"
files = [
    "net/proto2/bridge/proto/message_set.proto",
    "wireless/android/play/playlog/proto/clientanalytics.proto",
    "wireless/android/play/playlog/proto/personal_application_event.proto",
    "wireless/android/play/playlog/proto/personal_recorder.proto",
    "wireless/android/play/playlog/proto/play_games_client.proto",
    "wireless/android/play/playlog/proto/play_store_client.proto"
]

# Substitutions are applied in sequence, so later substitutions see effects of
# earlier ones. This is leveraged to gracefully handle the 'default' options.
substitutions = [
    # Remove the field option declaration.
    ('extend proto2.FieldOptions {[^}]*}', ''),
    ('extend proto2.EnumValueOptions {[^}]*}', ''),

    # Remove options and imports that aren't applicable or supported.
    ('option java_api_version = 2;\n', ''),
    ('option py_api_version = 2;\n', ''),
    ('option cc_api_version = 2;\n', ''),
    ('option java_generate_equals_and_hash = true;\n', ''),
    ('option java_enable_dual_generate_mutable_api = true;\n', ''),
    ('import "java/com/google/apps/jspb/jspb.proto";\n', ''),
    ('import "logs/proto/logs_annotations/logs_annotations.proto";\n', ''),
    ('import "net/proto2/proto/descriptor.proto";\n', ''),

    # Remove the message-level logs_proto options.
    ('^\s*option \(logs_proto\.\w*\) = \w*;', ''),

    # Leave only default options; temporarily replace []'s with <<</>>>'s.
    ('\[[^]]*?default = ([^]\s,]+)[^]]*\]', '<<<\\1>>>'),

    # Leave bracketed expressions in comments alone.
    ('(^\s*//[^\n]*)\[', '\\1{{{'),

    # Zap anything between []'s.
    ('\[[^]]*?\]', ''),

    # Restore the []'s around default options for the fields.
    ('<<<(.*?)>>>', '[default = \\1]'),

    # Restore [ in comments.
    ('{{{', '['),

    # Add option optimize_for = LITE_RUNTIME
    ('(syntax = "proto2";\n)', '\\1\noption optimize_for = LITE_RUNTIME;\n'),

    # Zap unnecessary whitespace (including newlines). This will sometimes cause
    # lines to become longer than 80 characters, but this is deemed an
    # acceptable style violation since fixing this properly requires more time
    # investment than the payoffs justify.
    ('([0-9])\s+;', '\\1;'),
    ('([0-9])\s+\[', '\\1 ['),

    # Zap trailing whitespace.
    (' +$', ''),

    # Chomp consecutive newlines.
    ('\n\n\n+', '\n\n')
]

def update_proto(in_file_path, out_file_path):
  in_file = file(in_file_path)
  proto_source = in_file.read()
  in_file.close()

  for pattern, replacement in substitutions:
    regex = re.compile(pattern, re.MULTILINE | re.DOTALL)
    proto_source = regex.sub(replacement, proto_source)

  out_file = file(out_file_path, 'w')
  out_file.write(proto_source)
  out_file.close()

def main(args):
  script_path = os.path.realpath(sys.argv[0])
  scripts_dir = os.path.dirname(script_path)

  for f in files:
    in_file_path = "%s%s" % (base_google3_path, f)
    out_file_path = os.path.realpath(
        "%s/../imported_protos/src/%s" % (scripts_dir, f))
    update_proto(in_file_path, out_file_path)
    print "processed %s" % f

if __name__ == '__main__':
  sys.exit(main(sys.argv[1:]))
