function DashController($scope, $http, $resource) {
	$http.get('gui/reports/totals')
	    .success(function(d) {
	        $scope.totals = d;
	    });
}
