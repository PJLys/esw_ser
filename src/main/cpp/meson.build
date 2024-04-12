boost_regex_dep = meson.get_compiler('cpp').find_library('boost_regex')
thread_dep = dependency('threads')
jsoncpp_dep = dependency('jsoncpp')
protobuf_dep = dependency('protobuf')

protoc = find_program('protoc', required : true)
proto_gen = generator(protoc,
                output    : ['@BASENAME@.pb.cc', '@BASENAME@.pb.h'],
                arguments : ['--proto_path=@CURRENT_SOURCE_DIR@/../proto/', '--cpp_out=@BUILD_DIR@', '@INPUT@'])
proto_generated = proto_gen.process('./../proto/measurements.proto')

avro = find_program('avrogencpp', required : true)
avro_gen = generator(avro,
                output    : ['@BASENAME@.hh'],
                arguments : ['--input=@CURRENT_SOURCE_DIR@/../avro/measurements.avsc', '--output=@BUILD_DIR@/avromeasurements.hh', '--namespace=avromessage'])
avro_generated = avro_gen.process('./../avro/measurements.avsc')

srcs = ['dataset.cpp', 'main.cpp', 'measurementinfo.cpp', 'result.cpp', proto_generated, avro_generated]

executable('server', srcs,
  dependencies: [thread_dep, boost_regex_dep, jsoncpp_dep, protobuf_dep])