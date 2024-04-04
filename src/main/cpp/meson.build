boost_regex_dep = meson.get_compiler('cpp').find_library('boost_regex')
thread_dep = dependency('threads')
jsoncpp_dep = dependency('jsoncpp')
protobuf_dep = dependency('protobuf')

protoc = find_program('protoc', required : true)
gen = generator(protoc,
                output    : ['@BASENAME@.pb.cc', '@BASENAME@.pb.h'],
                arguments : ['--proto_path=@CURRENT_SOURCE_DIR@/../proto/', '--cpp_out=@BUILD_DIR@', '@INPUT@'])
generated = gen.process('./../proto/measurements.proto')
srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp', generated]

executable('server', srcs,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep, protobuf_dep])