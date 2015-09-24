function ReceivedController($scope, $resource, RegistrationsByState) {
	var SendInvoiceResource = $resource('gui/registrations/send-invoices', {});
	var DismissRegistrationsResource = $resource('gui/registrations/dismiss-registrations', {});
	var MarkAsCompleteResource = $resource('gui/registrations/mark-as-complete', {});

	RegistrationsByState.get({}, function(d) {
		$scope.registrations = d.received;
	});

	$scope.checkedRegistrations = function() {
		var checked = [];
		angular.forEach($scope.registrations, function(d) {
			if (d.checked) checked.push(d.id);
		});
		return checked;
	};

	$scope.sendInvoices = function() {
		var rs = $scope.checkedRegistrations();
		if (rs.length == 0) {
			$scope.alert = {
				style: "warning", 
				message: "No registrations selected."
			};
			return;
		}

		SendInvoiceResource.save({registrations: rs}, function(d) {
			$scope.alert = {
				style: "success", 
				message: d.message
			};
		});
	};

	$scope.dismissRegistrations = function() {
		var rs = $scope.checkedRegistrations();
		if (rs.length == 0) {
			$scope.alert = {
				style: "warning", 
				message: "No registrations selected."
			};
			return;
		}

		DismissRegistrationsResource.save({registrations: rs}, function(d) {
			$scope.alert = {
				style: "success", 
				message: d.message
			};
		});
	};

	$scope.markAsComplete = function() {
		var rs = $scope.checkedRegistrations();
		if (rs.length == 0) {
			$scope.alert = {
				style: "warning", 
				message: "No registrations selected."
			};
			return;
		}

		MarkAsCompleteResource.save({registrations: rs}, function(d) {
			$scope.alert = {
				style: "success", 
				message: d.message
			};
		});
	};
}