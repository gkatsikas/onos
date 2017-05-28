/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 ONOS GUI -- Metron Service Chain Manager View Module
 */
(function () {
    'use strict';

    // injected references
    var $log, $scope, $location, ks, fs, cbs, ns;

    var metrics = 4;

    var hasServiceChainId;

    var labels = new Array(1);
    var data = new Array(metrics);
    for (var i = 0; i < metrics; i++) {
        data[i] = new Array(1);
        data[i][0] = 0;
    }

    var max;

    function ceil(num) {
        if (isNaN(num)) {
            return 0;
        }
        var pre = num.toString().length - 1
        var pow = Math.pow(10, pre);
        return (Math.ceil(num / pow)) * pow;
    }

    function maxInArray(array) {
        var merged = [].concat.apply([], array);
        return Math.max.apply(null, merged);
    }

    angular.module('ovScdepl', ["chart.js"])
        .controller('OvScdeplCtrl',
        ['$log', '$scope', '$location', 'FnService', 'ChartBuilderService', 'NavService',

        function (_$log_, _$scope_, _$location_, _fs_, _cbs_, _ns_) {
            var params;
            $log = _$log_;
            $scope = _$scope_;
            $location = _$location_;
            fs = _fs_;
            cbs = _cbs_;
            ns = _ns_;

            params = $location.search();

            if (params.hasOwnProperty('scId')) {
                $scope.scId = params['scId'];
                hasServiceChainId = true;
            } else {
                hasServiceChainId = false;
            }

            cbs.buildChart({
                scope: $scope,
                tag: 'scdepl',
                query: params
            });

            $scope.$watch('chartData', function () {
                if (!fs.isEmptyObject($scope.chartData)) {
                    $scope.showLoader = false;
                    var length = $scope.chartData.length;
                    labels = new Array(length);
                    for (var i = 0; i < metrics; i++) {
                        data[i] = new Array(length);
                    }

                    $scope.chartData.forEach(
                        function (cm, idx) {
                            data[0][idx]  = cm.synthesis_time;
                            data[1][idx]  = cm.deployment_time;
                            data[2][idx]  = cm.monitoring_time;
                            data[3][idx]  = cm.reconfiguration_time;

                            labels[idx] = cm.label;
                        }
                    );
                }

                max = maxInArray(data)
                $scope.labels = labels;
                $scope.data = data;

                // Auto-scale y-axis
                $scope.options = {
                    scaleOverride : true,
                    scaleSteps : 10,
                    scaleStepWidth : ceil(max) / 10,
                    scaleStartValue : 0,
                    scaleFontSize : 18
                };
                $scope.onClick = function (points, evt) {
                    var label = labels[points[0]._index];
                    if (label) {
                        ns.navTo('scdepl', { scId: label });
                        $log.log(label);
                    }
                };

                if (!fs.isEmptyObject($scope.annots)) {
                    $scope.serviceChainIds = JSON.parse($scope.annots.serviceChainIds);
                }

                $scope.onChange = function (serviceChainId) {
                    ns.navTo('scdepl', { scId: serviceChainId });
                };
            });

            $scope.series = [
                'Synthesis (us)',  'Deployment (us)', 'Monitoring (us)', 'Reconfiguration (us)'
            ];
            $scope.labels = labels;
            $scope.data = data;

            $scope.chartColors = [
                '#286090',
                '#F7464A',
                '#46BFBD',
                '#97BBCD',
                '#FDB45C'
                // '#4D5360',
                // '#8c4f9f'
            ];
            Chart.defaults.global.colours = $scope.chartColors;

            $scope.showLoader = true;

            $log.log('OvScdeplCtrl has been created');
        }]);

}());
