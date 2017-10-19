function StripeController($scope, $window, $location, $http) {

    //http://localhost:9080/stripe.html#?account_secret=0f6f369c-b3da-11e7-957f-bbf58cbe212a

    $scope.accountSecret = $location.search().account_secret;
    console.log($scope.accountSecret);


    $http.get('api/stripe/account/' + $scope.accountSecret)
        .success(function(d) {
            $scope.data = d;
        });

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
            );
          }
        });

        handler.open({
            amount: $scope.data.amountDueOre
        });
        e.preventDefault();

        // Close Checkout on page navigation:
        $window.addEventListener('popstate', function() {
          handler.close();
        });
    }
}

StripeController.$inject = ['$scope', '$window', '$location', '$http'];
