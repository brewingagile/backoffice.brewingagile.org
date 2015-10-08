function OverviewController($scope, $resource) {
    $scope.bs = $resource('gui/reports/bundles', {}).query();
	$scope.i = $resource('gui/reports/individuals', {}).get();

	$scope.totalBundles = function() {
		return {
			actual: {
				conference: _.reduce($scope.bs, function(memo, b) { return memo + b.actual.conference; }, 0),
				workshop1: _.reduce($scope.bs, function(memo, b) { return memo + b.actual.workshop1; }, 0),
				workshop2: _.reduce($scope.bs, function(memo, b) { return memo + b.actual.workshop2; }, 0)
			},
			planned: {
				conference: _.reduce($scope.bs, function(memo, b) { return memo + b.planned.conference; }, 0),
				workshop1: _.reduce($scope.bs, function(memo, b) { return memo + b.planned.workshop1; }, 0),
				workshop2: _.reduce($scope.bs, function(memo, b) { return memo + b.planned.workshop2; }, 0)
			}
		};
	};
}
