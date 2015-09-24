function InvoicingController($scope, $resource, RegistrationsByState) {
	var MarkAsPaidResource = $resource('gui/registrations/mark-as-paid', {});

	RegistrationsByState.get({}, function(d) {
		$scope.registrations = d.invoicing;
	});

	$scope.checkedRegistrations = function() {
		var checked = [];
		angular.forEach($scope.registrations, function(d) {
			if (d.checked) checked.push(d.id);
		});
		return checked;
	};

	$scope.markAsPaid = function() {
		var rs = $scope.checkedRegistrations();
		if (rs.length == 0) {
			$scope.alert = {
				style: "warning", 
				message: "No registrations selected."
			};
			return;
		}

		MarkAsPaidResource.save({registrations: rs}, function(d) {
			$scope.alert = {
				style: "success", 
				message: d.message
			};
		});
	};
}