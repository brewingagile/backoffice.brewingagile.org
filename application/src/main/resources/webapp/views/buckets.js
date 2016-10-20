function BucketsController($scope, $window, $resource) {
    var Resource = $resource('gui/buckets/', {}, {save: {method: 'PUT', isArray: true}});
    $scope.buckets = Resource.query();

    $scope.remove = function(m) {
        $scope.buckets = _.without($scope.buckets, m);
    };

    $scope.add = function() {
        $scope.buckets = _.union($scope.buckets, [{}]);
    };

    $scope.toggleDeal = function(b) {
        if (b.deal) {
            b.deal = null;
            return;
        } else {
            b.deal = {
                price: 0
            };
            return;
        }
    };

    $scope.save = function(buckets) {
        $scope.success = null;
		$scope.error = null;
        Resource.save({}, buckets, function() {
            $scope.success = buckets.length + ' hinkar har sparats.';
        }, function() {
            $scope.error = "Misslyckades med att spara.";
        });
    };
}
