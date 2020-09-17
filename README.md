## Challenge Summary 
Let's pretend you're on a software engineering team that's building a human movement recognition platform called Motion Mapper for mobile devices. An important component of this platform detects the unique physical movements a person performs while answering a voice phone call on their mobile device.

Your task is to make a usable mobile app that collects user movement data so that your Data Science team can evaluate it. More specifically, your app should collect the sensor data from a mobile device that measures a user's movements when answering a phone call. Because there are many permutations for how a person can answer a phone call, let's keep this simple and assume that we're only interested in calls that begin with the device lying flat on a table and end with the phone being held up to to the user's ear. 

## Libraries/Patterns chosen
- Protobuf
	- Chosen for cross platform compatibility, small file sizes, ease of serialability, and it's really cool!
- RxJava
- MVVM 
 
## Build instructions
Download dependencies, install [protobuf compiler](https://github.com/protocolbuffers/protobuf/releases/tag/v3.12.4), from root project dir run `./compile_proto.sh`. Run project. 

## Further Considerations

- If we wanted to collect data samples of this movement from one million unique devices running our app in the future, what would be the best way to transfer the data from those devices to our Data Science team? 
  - Compress and fragment all of the protobuf files, encrypt with AES/RSA, batch upload with auth credentials as well as more detailed information like GPS, IP, etc.
- Since we are concerned about data privacy, what could we do to ensure that the data transfer is secure?
  - Encrypt data using a key exchange with encryption combination such as AES and RSA. (RSA has a limitation on how much data it can encrypt, hence AES would encrypt large data and its key would be encrypted with RSA)
- Is there anything you would have wanted to implement or improve in your code if you had more time?
  - Add instrumentation tests
   - Validate sensor data is received
   - RxJava is passing data along to the repo in a way that's expected
   - Stress tests on large amounts of sensor data
   - Validate protofbuf objects can successfully be serialized / deserialized / written to disk.
  - Fix a case where I used a static `Activity` that held a `Context`. 
  - Add GPS location to sensor data. 
- Describe any issues you had while working on this challenge.  
  - I had to use outgoing calls as to test trigger cases because there wasn't a way to simulate an incoming call on a physical device.
  - Protobuf is awesome but it took a couple of hours to setup.
