function RegistrationController($scope, $routeParams, $resource) {
	var RegistrationsResource = $resource('gui/registrations/:id', {id: "@id"});
    var BundlesResource = $resource('gui/buckets/', {}, {save: {method: 'PUT', isArray: true}});

    $scope.registration = RegistrationsResource.get({id: $routeParams.registrationId});
	$scope.bundles = BundlesResource.query();

	$scope.save = function() {
		$scope.registration.$save(function() {
			$scope.alert = {
				style: "success", 
				message: "Registration Saved"
			};
		});
	};
}
