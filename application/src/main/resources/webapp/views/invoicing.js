function InvoicingController($scope, $resource, RegistrationsByState) {
	var MarkAsPaidResource = $resource('gui/registrations/mark-as-paid', {});
	var AutoMarkAsPaidResource = $resource('gui/registrations/auto-mark-as-paid', {});

    function reload() {
        RegistrationsByState.get({}, function(d) {
            $scope.registrations = d.invoicing;
        });
	}

	reload();

	$scope.checkedRegistrations = function() {
		var checked = [];
		angular.forEach($scope.registrations, function(d) {
			if (d.checked) checked.push(d.id);
		});
		return checked;
	};

    $scope.autoMarkAsPaid = function() {
        AutoMarkAsPaidResource.save(function(d) {
            $scope.alert = {
                style: "success",
                message: d.message
            };
            reload();
        });
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