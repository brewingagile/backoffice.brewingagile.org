var services = angular.module('services', ['ngResource']);

services.factory('IEDetector', [function() {
  var agent = navigator.userAgent.toLowerCase();
  var details = (agent.indexOf('msie') != -1) ? parseInt(agent.split('msie')[1]) : false;
  return {
    isIE: (details !== false),
    version: (details !== false)?details:undefined
  };
}]);

var app = angular.module('app', ['ngResource', 'services']);

