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

    window.ZoomChartsLicense = "";
    window.ZoomChartsLicenseKey = "";

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
