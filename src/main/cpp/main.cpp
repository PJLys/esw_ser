#include <iostream>

#include <json/json.h>

#include "dataset.h"
#include "result.h"

#include <boost/asio.hpp>
#include <boost/algorithm/string.hpp>

#include "measurements.pb.h"

using namespace std;
using boost::asio::ip::tcp;

void processJSON(tcp::iostream& stream){
    Json::Value val;
    Json::Reader reader;

    std::vector<Dataset> datasets;
    std::vector<Result> results;

    /* Read json string from the stream */
    string s;
    getline(stream, s, '\0');

    /* Parse string */
    reader.parse(s, val);

    datasets.clear();
    results.clear();
    for (int i = 0; i < val.size(); i++) {
        datasets.emplace_back();
        datasets[i].Deserialize(val[i]);
        /* Calculate averages */
        results.emplace_back(datasets[i].getInfo(), datasets[i].getRecords()); 
    }

    /* Create output JSON structure */
    Json::Value out;
//    Json::FastWriter writer;
    Json::StyledWriter writer;
    for (int i = 0; i < results.size(); i++) {
        Json::Value result;
        results[i].Serialize(result);
        out[i] = result;
    }

    /* Send the result back */
    std::string output = writer.write(out);
    stream << output;
    if (!getenv("ESW_JSON_QUIET"))
        cout << output;
    else
        cout << "Quiet operation enabled" << endl;
}

void processAvro(tcp::iostream& stream){
    throw std::logic_error("TODO: Implement avro");
}

void processProtobuf(tcp::iostream& stream) {
    cout<<"Processing protobuf message"<<endl;
    while (1) {
        uint32_t size = 0;
    
        if(!stream.read(reinterpret_cast<char*>(&size), sizeof(size)))
        {
            if(stream.eof())
            {
                cout<<"End of stream reached"<<endl;
                break;
            }
            else
            {
                cout<<"Failed to read the size of the message"<<endl;
                break;
            }
        }

        size = ntohl(size);
        cout<<"Size of the message: "<< size <<endl;
        std::string message(size, '\0');
        if(!stream.read(&message[0], size))
        {
            cout<<"Failed to read the message"<<endl;
            break;
        }

        esw::pDataset incoming_message;
        esw::pResult outgoing_message;   

        /* Read the message from the stream */
        if (!incoming_message.ParseFromString(message))
        {
            throw std::logic_error("Failed to parse incoming message");
        }

        if(!incoming_message.IsInitialized())
        {
            throw std::logic_error("Incoming message is not initialized");
        }

        const esw::pMeasurementInfo& info = incoming_message.info();
        std::cout << "Measurement info: " << std::endl;
        std::cout << "\tID: " << info.id() << std::endl;
        std::cout << "\tTimestamp: " << info.timestamp() << std::endl;
        std::cout << "\tName: " << info.measurer_name() << std::endl;

        auto* outInfo = outgoing_message.mutable_info();
        outInfo->set_id(info.id());
        outInfo->set_timestamp(info.timestamp());
        outInfo->set_measurer_name(info.measurer_name());

        //Process the incoming message
        for(int i = 0; i < incoming_message.records_size(); i++)
        {        
            const esw::pDataset::pRecord& record = incoming_message.records(i);
            
            //Print the records to the console for debugging
            std::cout << "\nRecord " <<i << ": " << std::endl;
            std::cout << "\tData: " << record.data_type() << std::endl;
            std::cout << "\tValue: ";
            for (int j = 0; j < record.values_size(); j++)
            {
                std::cout << record.values(j) << " ";
            }
            std::cout << std::endl;

            //Calculate the average
            double sum = 0;
            for (int j = 0; j < record.values_size(); j++)
            {
                sum += record.values(j);
            }
            double avg = sum / record.values_size();
            std:: cout << "\tAverage: " << avg << std::endl;

            auto* outAverage = outgoing_message.add_averages();
            outAverage->set_data_type(record.data_type());
            outAverage->set_value(avg);        
        }
    
        std::cout << "\nOutgoing message: " << std::endl;
        std::cout << outgoing_message.DebugString() << std::endl;

        stream.clear(); //Reset the state of the stream

        uint32_t reply_size = htonl(outgoing_message.ByteSizeLong());

        if (!stream.write(reinterpret_cast<const char*>(&reply_size), sizeof(reply_size))) {
            cout << "Failed to write the size of the message" << endl;
            break; // Exit the loop on error
        }
        
        //Serialize the output message
        if (!outgoing_message.SerializeToOstream(&stream))
        {
            throw std::logic_error("Failed to serialize outgoing message");
        }

        cout << "Message sent!" << endl;
    }
}

int main(int argc, char *argv[]) {

    if (argc != 3) {
        cout << "Error: two arguments required - ./server  <port> <protocol>" << endl;
        return 1;
    }

    // unsigned short int port = 12345;
    unsigned short int port = atoi(argv[1]);

    // std::string protocol = "json";
    std::string protocol(argv[2]);
    boost::to_upper(protocol);
    try {
        boost::asio::io_service io_service;

        tcp::endpoint endpoint(tcp::v4(), port);
        tcp::acceptor acceptor(io_service, endpoint);

        while (true) {
            cout << "Waiting for message in " + protocol + " format..." << endl;
            tcp::iostream stream;
            boost::system::error_code ec;
            acceptor.accept(*stream.rdbuf(), ec);

            if(protocol == "JSON"){
                processJSON(stream);
            }else if(protocol == "AVRO"){
                processAvro(stream);
            }else if(protocol == "PROTO"){
                processProtobuf(stream);
            }else{
                throw std::logic_error("Protocol not yet implemented");
            }

        }

    }
    catch (std::exception &e) {
        std::cerr << "Exception: " << e.what() << std::endl;
        return 1;
    }

    return 0;
}
