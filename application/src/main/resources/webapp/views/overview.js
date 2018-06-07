function OverviewController($scope, $http, $location) {
    $http.get('gui/accounts/2/')
        .success(function(d) {
            $scope.accounts = d;
        });

    $scope.account = "";
    $scope.exists = function(account) {
        return false;
    };

    $scope.create = function(account) {
        $http.put('gui/accounts/' + encodeURIComponent(account), {})
            .success(function() {
                $location.path('/account/' + account);
            });
    }
}
