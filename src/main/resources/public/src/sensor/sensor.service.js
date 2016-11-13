angular
    .module('sensor')
    .service('sensorService', sensorService);

/* @ngInject */
function sensorService($http) {

    this.getSensors = getSensors;

    function getSensors() {
        return $http({
            method: 'GET',
            url: '/api/sensor/list',
            headers: {'Content-Type': 'application/json'}
        }).then(function (response) {
            return response.data;
        });
    }
}

