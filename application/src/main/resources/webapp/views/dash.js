function DashController($scope, $http, $resource) {
	$http.get('gui/dash/totals')
	    .success(function(d) {
	        $scope.totals = d;
	    });
}
