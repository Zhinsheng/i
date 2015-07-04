angular.module('app').config(function($stateProvider) {
  $stateProvider.state('service.general.country.built-in', {
    url: '/built-in',
    views: {
      'content@service.general.country': {
        templateUrl: 'html/service/country/built-in/index.html',
        controller: 'ServiceBuiltInController'
      }
    }
  })
    .state('service.general.country.built-in.bankid', {
      url: '/built-in/?code',
      parent: 'service.general.country',
      data: {
        region: null,
        city: null
      },
      resolve: {
        oService: function($stateParams, service) {
          return service;
        },
        oServiceData: function($stateParams, service) {
          var aServiceData = service.aServiceData;
          return aServiceData[0];
        },
        BankIDLogin: function($q, $state, $location, $stateParams, BankIDService) {
          return BankIDService.isLoggedIn().then(function() {
            return {loggedIn: true};
          }).catch(function() {
            return $q.reject(null);
          });
        },
        BankIDAccount: function(BankIDService, BankIDLogin) {
          return BankIDService.account();
        },
        processDefinitions: function(ServiceService, oServiceData) {
          return ServiceService.getProcessDefinitions(oServiceData, true);
        },
        processDefinitionId: function(oServiceData, processDefinitions) {
          var sProcessDefinitionKeyWithVersion = oServiceData.oData.oParams.processDefinitionId;
          var sProcessDefinitionKey = sProcessDefinitionKeyWithVersion.split(':')[0];

          var sProcessDefinitionName = "тест";
          angular.forEach(processDefinitions.data, function(value, key) {
            if (value.key == sProcessDefinitionKey) {
              sProcessDefinitionKeyWithVersion = value.id;
              sProcessDefinitionName = "(" + value.name + ")";
            }
          });

          return {
            sProcessDefinitionKeyWithVersion: sProcessDefinitionKeyWithVersion,
            sProcessDefinitionName: sProcessDefinitionName
          };
        },
        ActivitiForm: function(ActivitiService, oServiceData, processDefinitionId) {
          return ActivitiService.getForm(oServiceData, processDefinitionId);
        }
      },
      views: {
        'content@service.general.country': {
          templateUrl: 'html/service/country/built-in/bankid.html',
          controller: 'ServiceBuiltInBankIDController'
        }
      }
    })
    .state('service.general.country.built-in.bankid.submitted', {
      url: null,
      data: {id: null},
      onExit: function($state) {
        var state = $state.get('service.general.country.built-in.bankid.submitted');
        state.data = {id: null};
      },
      views: {
        'content@service.general.country': {
          templateUrl: 'html/service/country/built-in/bankid.submitted.html',
          controller: function($state, $scope) {
            $scope.state = $state.get('service.general.country.built-in.bankid.submitted');
          }
        }
      }
    });
});