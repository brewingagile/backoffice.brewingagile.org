function RegistrationController($scope, $routeParams, $resource) {
	var RegistrationsResource = $resource('gui/registrations/:id', {id: "@id"});
	var NametagPrintedResource = $resource('gui/registrations/mark-as-printed', {});
	var UnmarkNametagPrintedResource = $resource('gui/registrations/unmark-as-printed', {});
    var BundlesResource = $resource('gui/buckets/', {}, {save: {method: 'PUT', isArray: true}});

    function refresh() {
        $scope.registration = RegistrationsResource.get({id: $routeParams.registrationId});
    }

    refresh();
	$scope.bundles = BundlesResource.query();

    $scope.unmarkNametagPrinted = function() {
		UnmarkNametagPrintedResource.save({registrations: [$scope.registration.id]}, function(d) {
			$scope.alert = {
				style: "success",
				message: d.message
			};
			refresh();
		});
	};

	$scope.markNametagPrinted = function() {
		NametagPrintedResource.save({registrations: [$scope.registration.id]}, function(d) {
			$scope.alert = {
				style: "success",
				message: d.message
			};
			refresh();
		});
	};

	$scope.save = function() {
		$scope.registration.$save(function() {
			$scope.alert = {
				style: "success", 
				message: "Registration Saved"
			};
		});
	};
}
