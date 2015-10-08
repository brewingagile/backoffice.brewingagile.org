var filters = angular.module('filters', []);
var directives = angular.module('directives', []);
var services = angular.module('services', []);

services.factory('VersionNumber', ['$resource', function($resource) {
	return $resource("gui/versionnumber").get();
}]);
services.factory('AuthService', ['$resource', function($resource) {
	var R = $resource('gui/loggedin')
	return {
		get: function(callback) {
			var lia = {};
			R.get(function(d) {
				angular.copy(d, lia);
				if (callback !== undefined) callback(lia);
			});
			return lia;
		}
	};
}]);
