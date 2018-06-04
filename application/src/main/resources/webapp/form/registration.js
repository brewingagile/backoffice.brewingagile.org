function RegistrationController($scope, $http, $resource, $window, $timeout, $window, $location) {
    function queryParam(name) {
        var url = $window.location.href;
        name = name.replace(/[\[\]]/g, "\\$&");
        var regex = new RegExp("[?&]" + name + "(=([^&#]*)|&|#|$)"),
            results = regex.exec(url);
        if (!results) return null;
        if (!results[2]) return '';
        return decodeURIComponent(results[2].replace(/\+/g, " "));
    }
    $scope.accountSignupSecret = queryParam("account_signup_secret");

	var RegistrationResource = $resource("api/registration/1/", {});
	var Tickets = $resource("api/registration/1/tickets");
    $http.get('api/registration/1/config')
        .success(function(d) {
            $scope.config = d;
        });

    $scope.lastRegisteredName = "";

	$scope.loading = false;
	Tickets.get(function(d) {
	    $scope.tickets = d.tickets;
	});

	$scope.r = {
		participantName: "",
		participantEmail: "",
		dietaryRequirements: "",
		lanyardCompany: "",
		twitter: "",
		tickets: [],
		invoiceRecipient: "",
		invoiceAddress: ""
	};

    $scope.stripeData = null;
	$scope.paymentMethod = 'CREDIT_CARD'; // valid: 'CREDIT_CARD', 'ACCOUNT'

    $http.get('api/registration/1/account/' + $scope.accountSignupSecret)
        .success(function(d) {
            $scope.account = d;
            $scope.paymentMethod = 'ACCOUNT';
        });

	$scope.reset = function() {	
		$scope.success = false;
		$timeout(function() {
			document.getElementById("inputParticipantName").focus(); //this is not the angular way, I know.
		});
	};

    $scope.selectedTickets = function() {
        return _.filter($scope.r.tickets, function(x) { return x != "" && x != null });
    };

    $scope.stripeCheckout = function() {
       console.log("stripeCheckout")
       var amountInOre = 2000;
       
       var registration = mkRegistration($scope);
       var handler = StripeCheckout.configure({
          key: $scope.config.stripePublicKey,
          name: 'Brewing Agile',
          image: 'img/logo.png',
          locale: 'auto',
          zipCode: true,
          currency: 'sek',
          email: registration.email,
          token: function(token) {
            $http.post('api/registration/1/',
                {
                    stripe: token,
                    registration: registration
                }
            ).success(function(p) {
                if (p.success) $scope.lastRegisteredName = $scope.r.participantName;
                $scope.success = p.success;
                $scope.loading = false;
                Tickets.get(function(d) { $scope.tickets = d.tickets; });
            }).error(function(data, status, headers, config) {
                //TODO - syns inte!
                $scope.alert = {
                    style: "danger",
                    message: data.message
                };
            });
          }
        });

        handler.open({ amount: amountInOre });

        $window.addEventListener('popstate', function() {
          handler.close();
        });
    };

	$scope.register2 = function() {
        $scope.success = null;
		$scope.error = null;
		$scope.loading = true;
	    $http.post('api/registration/1/',
            {
                registration: mkRegistration($scope),
                invoice: mkInvoice($scope),
                accountSignupSecret: ($scope.account) ? $scope.account.accountSignupSecret : null
            }
         ).success(function(p) {
            if (p.success) $scope.lastRegisteredName = $scope.r.participantName;
            $scope.success = p.success;
            $scope.loading = false;
            Tickets.get(function(d) { $scope.tickets = d.tickets; });
        }).error(function(data, status, headers, config) {
            $scope.error = true;
            $scope.loading = false;
        });
	}

	$scope.register = function() {
	    console.log($scope.paymentMethod);
	    if ($scope.paymentMethod === 'CREDIT_CARD') {
	        $scope.stripeCheckout();
	        return;
	    }

	    $scope.register2();
	}

    function mkRegistration(scope) {
        return {
            name: scope.r.participantName,
            email: scope.r.participantEmail,
            dietaryRequirements: scope.r.dietaryRequirements,
            lanyardCompany: scope.r.lanyardCompany,
            twitter: scope.r.twitter,
            tickets: scope.selectedTickets()
        };
    }

    function mkInvoice(scope) {
        if ($scope.paymentMethod != 'INVOICE')
            return null;

        return {
            recipient: r.invoiceRecipient,
            address: r.invoiceAddress
        };
    }

  $scope.anyTicket = function(t) {
    return ($scope.selectedTickets().length > 0);
  };
}
