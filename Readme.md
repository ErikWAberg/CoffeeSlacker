todo


coffee slacker requires some sensor which registers that the coffee brewing has been initialized/completed and notifies the service


install & launch mongod


Nothing really hosted on website, but if you want to use/extend it:
install bower
>cd $project_root
>bower install

either use the "secrets"-config below or just edit the constants in the source code to your liking

Create dir 'secret'
make files
'application.properties' containing:
server.port=XYZ
slack.webHook=https://hooks.slack.com/services/something/something
slack.token=blablabla
slack.channel=coffeeslacker
slack.debugUser=peter.pan

if you want, you can use platformio and create
'esp8266buildFlags' containing:
export PLATFORMIO_BUILD_FLAGS="'-DWIFI_PASS=\"wifipassword\"' '-DWIFI_SSID=\"wifiname\"' '-DHTTP_HOST=\"service-ip\"''-DHTTP_PORT=XYZ'"


then run
./gradlew copySettings
source secret/esp8266buildFlags

compile stuff and stufferino
