function TicketsController($scope, $window, $resource) {
    var Resource = $resource('gui/tickets/', {}, {save: {method: 'PUT', isArray: true}});
    $scope.tickets = Resource.query();

    $scope.remove = function(m) {
        $scope.tickets = _.without($scope.tickets, m);
    };

    $scope.add = function() {
        $scope.tickets = _.union($scope.tickets, [{}]);
    };

    $scope.save = function(tickets) {
        $scope.success = null;
		$scope.error = null;
        Resource.save({}, tickets, function() {
            $scope.success = tickets.length + ' biljetter har sparats.';
        }, function() {
            $scope.error = "Misslyckades med att spara.";
        });
    };
}
