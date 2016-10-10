function OverviewController($scope, $resource) {
    $scope.bs = $resource('gui/reports/bundles', {}).query();
	$scope.i = $resource('gui/reports/individuals', {}).get();
	$scope.totals = $resource('gui/reports/totals', {}).get();
}
