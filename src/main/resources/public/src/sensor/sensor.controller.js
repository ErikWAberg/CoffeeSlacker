angular
    .module('sensor')
    .controller('SensorController', SensorController);


/* @ngInject */
function SensorController($scope, sensorService) {
    var vm = this;
    vm.sensors = {};
    vm.getSensors = getSensors();

    function getSensors() {
        return sensorService.getSensors().then(function(data) {
            vm.sensors = data;
            return vm.sensors;
        });
    }

}
