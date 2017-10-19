function StripeController($scope, $window, $location, $http) {
    $scope.accountSecret = $location.search().account_secret;

    function reload() {
        $http.get('api/stripe/account/' + $scope.accountSecret)
            .success(function(d) {
                $scope.data = d;
            });
    }

    $scope.checkout = function() {
        var handler = StripeCheckout.configure({
          key: $scope.data.key,
          name: 'Brewing Agile',
          image: 'img/logo.png',
          locale: 'auto',
          zipCode: true,
          currency: 'sek',
          token: function(token) {
            $http.post('api/stripe/account/' + $scope.accountSecret + "/pay",
                {
                    token: {
                        id: token.id,
                        email: token.email
                    },
                    amount: $scope.data.amountDueOre
                }
            ).success(function() {
                $scope.alert = {
                    style: "success",
                    message: "Payment was successful."
                };
                reload();
            }).error(function(data, status, headers, config) {
                $scope.alert = {
                    style: "danger",
                    message: data.message
                };
            });
          }
        });

        handler.open({
            amount: $scope.data.amountDueOre
        });
        e.preventDefault();

        $window.addEventListener('popstate', function() {
          handler.close();
        });
    }

    reload();
}

StripeController.$inject = ['$scope', '$window', '$location', '$http'];
