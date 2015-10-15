function RegistrationController($scope, $routeParams, $resource) {
	var RegistrationsResource = $resource('gui/registrations/:id', {id: "@id"});
	var NametagPrintedResource = $resource('gui/registrations/mark-as-printed', {});
    var BundlesResource = $resource('gui/buckets/', {}, {save: {method: 'PUT', isArray: true}});

    $scope.registration = RegistrationsResource.get({id: $routeParams.registrationId});
	$scope.bundles = BundlesResource.query();

	$scope.markNametagPrinted = function() {
		NametagPrintedResource.save({registrations: [$scope.registration.id]}, function(d) {
			$scope.alert = {
				style: "success",
				message: d.message
			};
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
