function DashController($scope, $http, $resource) {
    $http.get('gui/accounts/2/')
        .success(function(d) {
            $scope.accounts = d;
        });

    $scope.bs = $resource('gui/reports/bundles', {}).query();
	$scope.i = $resource('gui/reports/individuals', {}).get();
	$scope.totals = $resource('gui/reports/totals', {}).get();
}
