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

    $http.get('api/registration/1/account/' + $scope.accountSignupSecret)
        .success(function(d) {
            $scope.account = d;
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

	$scope.submit = function() {
		$scope.success = null;
		$scope.error = null;
		$scope.loading = true;
		var r = $scope.r;
		RegistrationResource.save(
		    {
        		participantName: r.participantName,
        		participantEmail: r.participantEmail,
        		dietaryRequirements: r.dietaryRequirements,
        		lanyardCompany: r.lanyardCompany,
        		twitter: r.twitter,
        		tickets: $scope.selectedTickets(),
        		invoiceRecipient: r.invoiceRecipient,
        		invoiceAddress: r.invoiceAddress,
        		billingMethod: "EMAIL",
        		accountSignupSecret: ($scope.account) ? $scope.account.accountSignupSecret : null
        	}
		, function(p) {
			if (p.success) $scope.lastRegisteredName = $scope.r.participantName;
			$scope.success = p.success;
			$scope.loading = false;

			Tickets.get(function(d) { $scope.tickets = d.tickets; });
		}, function(response) { 
			$scope.error = true;
			$scope.loading = false;
		});
	};

  $scope.showForm = function() {
    if ($scope.success) return false;
    return true;
  }

  $scope.anyTicket = function(t) {
    return ($scope.selectedTickets().length > 0);
  };
}
