define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager'], function($, data, index, widgetManager) {
  class SankeyWidget {
        elementId
        orderType
        widgetSettings

        constructor(elementId, orderType, widgetSettings) {
          this.elementId = elementId;
          this.orderType = orderType;
          this.widgetSettings = widgetSettings;
          console.log("Creating sankey with settings: " + JSON.stringify(widgetSettings))
          this.setup();
        }

        setup() {
          console.log('Preparing to draw sankey chart for element ID ' + this.elementId + ' with node ordering type ' + this.orderType);
          
          index.getReadySemaphor().done(() => {
            console.log('Chart drawing has green light from the central index semaphor, requesting for dataSessionId ' + this.widgetSettings.dataSessionId);
            data.awaitData('relationCounts', this.widgetSettings.dataSessionId).done(() => {
              console.log('Loading data was successful. Checking if the input column also gives green light.');
              const inputcolReadyPromise = $('#inputcol').data('animationtimer');
              if (inputcolReadyPromise) {
                inputcolReadyPromise.done(() => {
                  console.log("Input column gives green light")
                    this.init(this.elementId, this.orderType);
                });
              } else {
                this.init(this.elementId, this.orderType);
              }
            });
          });
        }

        init() {
          console.log('Initializing sankey chart');
          const sankeyDat = data.getData('relationCounts');
          console.log(sankeyDat);
          this.preprocessed_data = data.preprocess_data(sankeyDat, this.orderType);

          this.settings = {
            width: 500,
            height: 300,
            min_height: 200,
            padding_x: 0,
            padding_y: 20,
            node_spacing: 7,
            min_node_height: 5,
            label_font_size: 12,
            node_width: 10,
            node_to_label_spacing: 5,
            // max_number_nodes: 3,
            show_other: false,
            restrict_other_height: true,
            max_other_height: 100,
          };


          let running = false;
          //window.addEventListener('resize',() => {
          //  if (!running) {
          //    running = true;
          //    window.setTimeout(() => {this.redraw.bind(this)();running = false;}, 1000);
          //  }
          //});


          this.selected_by_node_id = {};


          this.main();
        }

        main() {
          if (!$('#' + this.elementId).data('mainWasCalled')) {
            const settings = this.settings;
            // Remove the Loading... banner
            $('#' + this.elementId + '-outer .text-center.shine').remove();

            this.redraw();

            this.add_slider('padding-slider', 'Padding: ', 2, 25, 2, settings.node_spacing, (value) => settings.node_spacing = Number(value));
            this.add_slider('min-size-slider', 'Minimum node size: ', 5, 25, 2, settings.min_node_height, (value) => settings.min_node_height = value);
            //this.add_slider('node-height-slider', 'Chart height: ', 40, 400, 2, settings.height, (value) => settings.height = value - 0);
            // add_slider("node-number-slider", "Max number of nodes: ", 0, 300, 2, this.settings.max_number_nodes, (value) => this.settings.max_number_nodes = value);
            this.add_slider('max-other-slider', 'Maximum size of "Other" node:', 2, 150, 2, settings.max_other_height, (value) => settings.max_other_height = value);

            this.add_toggle(
                'restrict-other-toggle',
                'Restrict size of "Other" node',
                settings.restrict_other_height,
                (state) => settings.restrict_other_height = state,
            );

            this.add_toggle(
                'show-other-toggle',
                'Show "Other" node',
                settings.show_other,
                (state) => settings.show_other = state,
            );

            this.add_button('Clear selection', () => {
              this.selected_by_node_id = {};
              this.redraw();
            });
            
            $('#' + this.elementId).data('mainWasCalled', true);
            console.log('Finished main.');
          } else {
            console.log('Not executing sankeychart#main() again because it had already been called');
          }
        }

        create_svg() {
          const chart_elem = document.getElementById(this.elementId);
          const chart = d3.select(chart_elem);

          chart.selectAll('svg').remove();

          const chartContainer = $('#' + this.elementId + '-container');
          // chartContainer.closest(".panel-body > .shine").addClass("hidden");
          chartContainer.removeClass('hidden');
          this.settings.width = chart_elem.clientWidth - 2 * this.settings.padding_x - 10;
          this.settings.height = chart_elem.clientHeight - 2 * this.settings.padding_y;
          console.log("Creating svg element with size " + this.settings.width + " x " + this.settings.height)
          const svg = chart
              .append('svg')
              .attr('width', this.settings.width)
              .attr('height', this.settings.height)
              .attr('top', '50px');

          return svg;//.append('g');//.attr('transform', 'translate(' + this.settings.padding_x + ',' + this.settings.padding_y + ')');
        }


        redraw() {
          if (this.widgetSettings.viewMode === 'small') {
            $('#'+this.elementId).parent().removeClass((index, classNames) => classNames.split(' ').filter(name => name.match('.*col-[0-9]*'))).addClass('col-12');
            $('#'+this.elementId+'-container').parent().addClass('d-none');
          } else {
            $('#'+this.elementId).parent().removeClass((index, classNames) => classNames.split(' ').filter(name => name.match('.*col-[0-9]*'))).addClass('col-10');
            $('#'+this.elementId+'-container').parent().removeClass('d-none');
          }

          const svg = this.create_svg();

          let max_other_height;
          if (this.settings.restrict_other_height) {
            max_other_height = Number(this.settings.max_other_height);
          } else {
            max_other_height = Infinity;
          }
          const the_data = data.prepare_data(this.preprocessed_data, this.settings.height, this.settings.min_node_height, this.settings.node_spacing, this.settings.show_other, max_other_height);
          console.log('Finished preparing data');

          const sankey = d3.sankey();

          console.log("Sankey size set to " + this.settings.width + " x " + this.settings.height + " with origin (0, 0).")
          sankey
              .size([this.settings.width - 1 , this.settings.height - 5])
              .nodeWidth(this.settings.node_width)
              .nodePadding(this.settings.node_spacing)
              .nodeId((d) => d.id)
              .nodes(the_data.nodes)
              .links(the_data.links);
              //.iterations(0);

          console.log('Computing sankey layout...');
          sankey();
          console.log('Done laying out sankey.');

          // shift(the_data.nodes[4], 50, 20);

          sankey.update(the_data);

          this.adapt_node_widths(the_data, max_other_height);

          // links
          const links = svg.append('g')
              .attr('fill', 'none')
              .attr('stroke', '#000')
              .attr('fill-opacity', '0.2')
              .selectAll('path.link')
              .data(the_data.links)
              .enter().append('path')
              .attr('class', 'link')
              .attr('fill', (d) => d.color)
          // .attr("stroke", (d) => d.color)
              .attr('d', this.compute_path)
              .attr('stroke-width', 0);
            // .attr("stroke-width", (d) => d.width);

          links.append('title')
              .text((link) => [link.source.id, link.target.id, link.color].join());

          // nodes
          const nodes = svg.append('g')
              .selectAll('.node')
              .data(the_data.nodes)
              .join("rect")
                .attr('class', 'node')
                .attr("x", d => d.x0)
                .attr("y", d => d.y0)
                .attr("height", d => d.y1 - d.y0)
                .attr("width", d => d.x1 - d.x0)
                .attr("fill", (d) => {
                /* COLOR SCHEME FOR NODES
                    normal - black
                    misc/hidden - gray (semi-transparent)
                    selected - blue
                    pinned - ? maybe a pattern? red checkerboard?

                     */

                if (this.selected_by_node_id[d.id]) {
                  return '#0040a0';
                } else {
                  return '#000000';
                }})
                .attr('opacity', (d) => {
                if (d.id === 'MISC_from' || d.id === 'MISC_to') {
                  return 0.4;
                } else {
                  return 1;
                }
              })
            .append("title")
              .text(d => d.name);

          // nodes: rects
          // nodes.append('rect')
          //     .attr('height', (d) => d.y1 - d.y0)
          //     .attr('width', (d) => d.x1 - d.x0)
          //     .attr('opacity', (d) => {
          //       if (d.id === 'MISC_from' || d.id === 'MISC_to') {
          //         return 0.4;
          //       } else {
          //         return 1;
          //       }
          //     })
          //     .attr('fill', (d) => {
          //        COLOR SCHEME FOR NODES
          //           normal - black
          //           misc/hidden - gray (semi-transparent)
          //           selected - blue
          //           pinned - ? maybe a pattern? red checkerboard?

                     

          //       if (this.selected_by_node_id[d.id]) {
          //         return '#0040a0';
          //       } else {
          //         return '#000000';
          //       }
          //     });

          nodes.on('click', (d) => {
            this.selected_by_node_id[d.id] = !this.selected_by_node_id[d.id];
            this.redraw();
          });

          const settings = this.settings;
          // nodes: labels
          svg.append('g')
              .attr('font-size', this.settings.label_font_size + 'px')
              .attr('font-family', 'sans-serif')
              .selectAll('text')
              .data(the_data.nodes)
              .join("text")
                .text((d) => d.name)
                .attr('y', (d) => (d.y1 + d.y0) / 2)
                .attr('x', (d) => d.x0 < this.settings.width / 2 ? d.x1 + 6 : d.x0 - 6)
                .attr("dy", "0.35em")
                .attr("text-anchor", d => d.x0 < this.settings.width / 2 ? "start" : "end");

          // nodes.append('title')
              // .text((d) => d.name);


          // vertical centering in the widget
          //let height = parseInt($('#' + this.elementId).css('height').slice(0, -2));
          //let parentHeight = parseInt($('#' + this.elementId).parent().css('height').slice(0, -2));
          //let parentWidth = parseInt($('#' + this.elementId).parent().css('width').slice(0, -2));
          //let headerHeight = parseInt($('#'+this.elementId+'-outer .card-header').css('height').slice(0, -2));
          //$('#' + this.elementId).css('position', 'absolute');
          //$('#' + this.elementId).css('top', (headerHeight+((parentHeight-height)/2))+'px');
          //$('#' + this.elementId).css('width', (parentWidth*2/3)+'px');
        }

        add_toggle(id, text, initial_state, change_handler) {
          const p = d3.select('#' + this.elementId + '-container .settings .checkboxes').append('p');
          const input = p.append('input')
            .attr('type', 'checkbox')
            .attr('class', 'btn-check')
            .attr('id', id);
          if (initial_state) {
            input.attr('checked', 'checked');
          }
          p.append('label')
            .attr('for', id)
            .attr('class', 'btn btn-primary')
            .text(' ' + text);
          const redraw = this.redraw.bind(this);
          input.on('change', function() {
            change_handler(this.checked);
            redraw();
          });
        }

        add_slider(id, label_text, min, max, step, value, change_handler) {
          const p = d3.select('#' + this.elementId + '-container .settings').select('.sliders').append('p');
          const redraw = this.redraw.bind(this);


          p.append('label')
              .attr('for', id)
              .text(label_text);

          p.append('input')
              .attr('type', 'range')
              .attr('id', id)
              .attr('min', min)
              .attr('max', max)
              .attr('step', step)
              .attr('value', value)
              .on('input', function() {
                const value = this.value;
                change_handler(value);
                redraw();
              });
        }

        add_button(text, click_handler) {
          d3.select(this.elementId).select('.buttons')
              .append('button')
              .text(text)
              .on('click', click_handler);
        }

        shift(node, x, y) {
          node.x0 += x;
          node.x1 += x;
          node.y0 += y;
          node.y1 += y;
        }

        compute_path(d) {
          const x_left = d.source.x1;
          const x_right = d.target.x0;
          const x_center = (x_left + x_right) / 2;
          const left_width = d.left_width;
          const right_width = d.right_width;

          /* Thick, stroked path
            let path =
                "M " +
                x_left + ", " +
                d.y0 +
                " C " +
                x_center + ", " +
                d.y0 + ", " +
                x_center + ", " +
                d.y1 + ", " +
                x_right + ", " +
                d.y1;
            */
          // filled path
          const path =
                'M ' +
                x_left + ', ' +
                (d.y0 - left_width / 2) +
                ' L ' +
                x_left + ', ' +
                (d.y0 + left_width / 2) +
                ' C ' +
                x_center + ', ' +
                (d.y0 + left_width / 2) + ', ' +
                x_center + ', ' +
                (d.y1 + right_width / 2) + ', ' +
                x_right + ', ' +
                (d.y1 + right_width / 2) +
                ' L ' +
                x_right + ', ' +
                (d.y1 - right_width / 2) +
                ' C ' +
                x_center + ', ' +
                (d.y1 - right_width / 2) + ', ' +
                x_center + ', ' +
                (d.y0 - left_width / 2) + ', ' +
                x_left + ', ' +
                (d.y0 - left_width / 2);

          return path;
        }

        adapt_node_widths(data, max_other_height) {
          let left_other_y0 = 0;
          let left_scale = 1;
          let right_other_y0 = 0;
          let right_scale = 1;

          for (const node of data.nodes) {
            if (node.id === 'MISC_from') {
              left_other_y0 = node.y0;
              if (node.y1 - node.y0 > max_other_height) {
                left_scale = max_other_height / (node.y1 - node.y0);
                node.y1 = node.y0 + max_other_height;
              }
            }
            if (node.id === 'MISC_to') {
              right_other_y0 = node.y0;
              if (node.y1 - node.y0 > max_other_height) {
                right_scale = max_other_height / (node.y1 - node.y0);
                node.y1 = node.y0 + max_other_height;
              }
            }
          }

          for (const link of data.links) {
            if (link.source.id === 'MISC_from') {
              link.left_width = left_scale * link.width;
              link.y0 = left_other_y0 + left_scale * (link.y0 - left_other_y0);
            } else {
              link.left_width = link.width;
            }
            if (link.target.id === 'MISC_to') {
              link.right_width = right_scale * link.width;
              link.y1 = right_other_y0 + right_scale * (link.y1 - right_other_y0);
            } else {
              link.right_width = link.width;
            }
          }
        }
  }

  return function newSankeyWidget(elementId, orderType, widgetsettings) {
    widgetManager.addWidget(widgetsettings.widgetId, new SankeyWidget(elementId, orderType, widgetsettings));
  };
});
