function BudgetController($scope, $window, $resource) {
    var FixedCosts = $resource('gui/budget/fixed-costs', {}, {save: {method: 'PUT', isArray: true}});
    $scope.costs = FixedCosts.query();

    $scope.remove = function(m) {
        $scope.costs = _.without($scope.costs, m);
    };

    $scope.add = function() {
        $scope.costs = _.union($scope.costs, [{}]);
    };

    $scope.save = function(costs) {
        $scope.success = null;
		$scope.error = null;
        FixedCosts.save({}, costs, function() {
            $scope.success = costs.length + ' "fixed costs" har sparats.';
        }, function() {
            $scope.error = "Misslyckades med att spara.";
        });
    };
}
