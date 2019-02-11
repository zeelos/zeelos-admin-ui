<template>
    <div>
        <div id="topology"></div>
        <div id="popup"
             style="z-index:9999; display:none; position:absolute; background: #eee; border:1px solid #09c; padding:10px; pointer-events:none"></div>
    </div>
</template>

<script>
    // loading css asynchronously
    import('@dvsl/zoomcharts/lib/assets/zc.css')
    import zc from '@dvsl/zoomcharts'

    window.ZoomChartsLicense = "ZCS-1zx51p9ww: ZoomCharts SDK Single Developer Licencefor chr..@..os.io (valid for development only); upgrades until: 2019-07-23";
    window.ZoomChartsLicenseKey = "610c8108ff8ed7b25d96d204d9fba95614d41ba77a95bd1609" +
        "31a147a120d9008e5552ee3a74ae92047eeb5442546b5ca00fe1c6e18be2aef5d6b126150743b" +
        "fe15cac164438414d04846ff2e36c4732074f2b0d81ce7c099a224039bf9e9e48903a6bc651d8" +
        "685d9a5396d294ad16577774148c323df1c4bc81e1a7c8b1d0c566356c33edd72d2c50f802980" +
        "64dd59a6d0ca6992856a30e2ad2a694bae11f9bec516d53f2791a3a9b0e403d597131631530e3" +
        "6685acb5c9ee5e39054fd3790afc7c0a38a474a2991ee4c5fdfd2beba311b71835a44fc255d0f" +
        "cdf32a9cd721846f2a8a16c48e6f4bd3e7cd329885e5bb6c846b90efc84a943ced779e58158b1";

    var zoomchartsIdsIgnore = ["loaded", "resetCoordinates", "resetLocked"];

    export default {
        name: "netchart",
        data() {
            return {
                netChart: null,
                pieChart: null,
                lastPieChartNode: null,
                lastSettings: null,
                currentNode: null,
            }
        },
        mounted: function () {
            let NetChart = zc.NetChart;

            this.infoElementVisible = false;
            this.infoElement = document.getElementById("popup");

            var chartContainer = document.getElementById("topology");

            this.netChart = new NetChart({
                container: chartContainer,
                area: {height: 630},
                data: {url: "http://localhost:8080/resources/assets", requestTimeout: 240000},
                navigation: {
                    mode: "focusnodes",
                    focusNodeExpansionRadius: 1,
                    numberOfFocusNodes: 1,
                    focusNodeTailExpansionRadius: 1,
                    initialNodes: ["1"],
                    minNumberOfFocusNodes: 1
                },
                layout: {
                    aspectRatio: true,
                    nodeSpacing: 50
                },
                style: {
                    nodeStyleFunction: this.nodeStyle,
                    linkStyleFunction: this.linkStyle
                },
                events: {
                    onClick: this.graphClick,
                    onHoverChange: this.hoverChanged//
                },
                advanced: {
                    pointer: {
                        noClickOnDoubleClick: false
                    }
                }
            });

            // attach event handlers that move the info element with the mouse cursor.
            chartContainer.addEventListener("mousemove", this.movePopup, true);
            chartContainer.addEventListener("pointermove", this.movePopup, true);
        },
        methods: {
            graphClick(event, args) {
                if (!event.ctrlKey && !event.shiftKey && args.clickNode) {
                    var self = this;
                    setTimeout(function () {
                        self.netChart.addFocusNode(args.clickNode);
                    }, 100);
                }
            },
            hoverChanged(event, args) {
                var content = "";

                var data;
                if (args.hoverItem) {
                    data = args.hoverItem.data;
                } else if (args.hoverNode) {
                    data = args.hoverNode.data;
                } else if (args.hoverLink) {
                    data = args.hoverLink.data;
                }

                if (data) {
                    Object.keys(data)
                        .sort() // sort the details object keys
                        .forEach(key => {
                            if (zoomchartsIdsIgnore.indexOf(key) === -1) // hide zoomchart id's
                                content += "<b>" + key +"</b>" +  ": " + data[key] + "<br>";
                        });

                    this.infoElement.innerHTML = content;
                }

                this.infoElementVisible = !!content;
                this.infoElement.style.display = this.infoElementVisible ? "block" : "none";
            },
            movePopup(event) {
                this.infoElement.style.top = event.pageY + "px";
                this.infoElement.style.left = event.pageX-250 + "px";
            },
            nodeStyle(node) {
                var image = null;
                if (node.data.type === "Root") {
                    //image = "";
                } else if (node.data.type === "Servers") {
                    image = "/static/img/gateway.png";
                } else if (node.data.type === "Endpoints") {
                    image = "/static/img/sensor.png";
                } else if (node.data.type === "Objects") {
                    //image = "/static/img/leshan-small.png";
                }

                //var sliceNo = (node.data.foreign) ? 1 : 0;
                //var sliceSize = 239;
                node.image = image;
                //node.imageSlicing = [0, sliceNo * sliceSize, sliceSize, sliceSize];

                node.label = node.data.name;

                //add plus marks on nodes that have hidden links
                if (node.dataLinks.length > node.links.length) {
                    node.items = [{px: 0, py: -1, text: "+", backgroundStyle: {fillColor: "orange"}}];
                } else {
                    node.items = [];
                }
            },
            linkStyle(link) {
                if (link.data.name === "hasGateway") {
                    link.fillColor = "lightgray";
                    link.radius = 5;
                } else if (link.data.name === "hasEndpoint") {
                    link.fillColor = "limegreen";
                    link.radius = 2;
                    link.fromDecoration = "circle";
                    link.toDecoration = "arrow";
                }

                if (link.data.name !== "hasGateway")
                    link.label = link.data.name;
            }
        }
    }
</script>
