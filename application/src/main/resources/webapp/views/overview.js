function OverviewController($scope, $http) {
    $http.get('gui/accounts/2/')
        .success(function(d) {
            $scope.accounts = d;
        });
}
