define(['jquery', 'gepi/charts/data', 'gepi/pages/index', 'gepi/components/widgetManager'], function($, data, index, widgetManager) {
  class CircleChartWidget {
        elementId
        widgetSettings
        settings
        links
        nodes
        hovered_id = ''

        constructor(elementId, widgetSettings) {
          this.elementId = elementId;
          this.widgetSettings = widgetSettings;
          this.settings = {
            radius: 150,
            min_radius: 120,
            node_count: 75,
            padding: 90,
            node_spacing: 10,
            node_thickness: 10,
            default_grey: false,
            fine_node_highlights: true,
            active_link_color: '#000055',
            inactive_link_color: '#aaa',
            active_node_opacity: 1,
            inactive_node_opacity: 0.15,
          };
          this.setup();
        }

        setup() {
          console.log('Preparing to draw circle chart for element ID ' + this.elementId);
          index.getReadySemaphor().done(() => {
            console.log('Chart drawing has green light from the central index semaphor, requesting data for dataSessionId ' + this.widgetSettings.dataSessionId);
            data.awaitData('relationCounts', this.widgetSettings.dataSessionId).done(() => {
              console.log('Loading data was successful. Checking if the input column also gives green light.');
              const inputcolReadyPromise = $('#inputcol').data('animationtimer');
              if (inputcolReadyPromise) {
                inputcolReadyPromise.done(() =>
                  this.first_draw());
              } else {
                this.first_draw();
              }
            });
          });
        }


        first_draw() {
          if (!$('#'+this.elementId).data('firstDrawn')) {
            let running = false;
            window.onresize = () => {
              if (!running) {
                running = true;
                console.log("Trying to call theDraw")
                this.draw(this.elementId);
                console.log("After trying to call theDraw")
                running = false;
              }
            };

            $('#' + widgetManager.getWidget('circlechart-outer').handleId).click(function() {
              this.draw(this.elementId);
            });

            this.add_toggle(
                this.elementId,
                'default-gray-toggle',
                'Grey out nodes and links by default',
                this.settings.default_grey,
                (state) => this.settings.default_grey = state,
            );

            this.add_toggle(
                this.elementId,
                'fine-opacity-toggle',
                'Highlight nodes based on edge weight',
                this.settings.fine_node_highlights,
                (state) => this.settings.fine_node_highlights = state,
            );

            // add_slider(this.elementId, "size_slider", "Size of the diagram: ", 50, 300, 5, this.settings.node_count, (count) => {
            //     this.settings.node_count = count;
            //     this.settings.radius = 2 * count;
            // });

            this.draw(this.elementId);
            $('#'+this.elementId).data('firstDrawn', true);
          } else {
            console.log('Not executing circleshart#first_draw() because it has already been run.');
          }
        }

        prepare_data(links) {
          // input: links der form (sourceId, targetId, frequency)
          // let {raw_nodes, links} = data();
          const node_indices = {};

          let total_weight = 0;
          const node_weights = {};
          const node_weight_target = new Map();
          const nodes = [];
          const raw_nodes = [];
          // node_count is defined below because we need to know
          // how many nodes there are before setting the count

          for (const {
            source,
            target,
            frequency,
          } of links) {
            total_weight += frequency;

            function add_to_node(node, w) {
              if (node_weights[node] === undefined) {
                node_weights[node] = 0;
                const index = raw_nodes.length;
                raw_nodes.push(node);
                node_weight_target.set(node, 0);
              }
              node_weights[node] += w;
            }
            add_to_node(source, frequency);
            add_to_node(target, frequency);
            node_weight_target.set(target, node_weight_target.get(target) + frequency);
          }
          // jetzt haben wir node_weights der form nodeId -> frequencySum
          // raw_nodes: [nodeId]

          const node_count = Math.min(this.settings.node_count, node_weight_target.size);

          const sorted_nodes_and_weights = Array.from(Object.entries(node_weights)).sort(([n1, w1], [n2, w2]) => w2 - w1).slice(0, node_count);
          const included_nodes = {};

          for (let [n, w] of sorted_nodes_and_weights) {
            included_nodes[n] = true;
          }
          // now we have included_nodes: nodeId -> true
          // of 100 most frequent nodes

          console.log('Included nodes: ', included_nodes);

          links = links.filter(({
            source,
            target,
          }) => {
            return included_nodes[source] && included_nodes[target];
          });
          // die links sind jetzt nur noch diejenigen zwischen den included nodes

          // distribute the nodes evenly across a circle
          const node_distance = 360 / node_count;

          // raw_nodes: [nodeId] (i.e. index below is just the array index)
          // node_weights: nodeId -> frequencySum
          // nodes is empty until now
          // psi: Fibonacci-Konstante; sorgt für eine gleichmaessige
          // Verteilung der Knoten auf dem Kreis, wobei die grossen
          // (eben die ersten) Knoten ungefaehr ihre urspruengliche
          // Position behalten und die letzteres Knoten dazwischen
          // verteilt werden.
          const psi = (Math.sqrt(5) - 1) / 2;
          let next_index = 0;
          const offset = Math.round(psi * node_count);
          for (const [id, weight] of sorted_nodes_and_weights) {
            next_index = (next_index + offset) % node_count;
            while (nodes[next_index % node_count]) {
              next_index += 1;
              if (next_index > 2*node_count) {
                console.log('Critical error!');
                break;
              }
            }
            next_index %= node_count;

            const weight_target = node_weight_target[id];
            const weight_ratio = weight_target / weight;

            nodes[next_index] = {
              id,
              pos: next_index * node_distance,
              weight,
              weight_ratio,
            };
            node_indices[id] = next_index;
          }
          // now we have
          // nodes: [{nodeId, pos, weight, weight_ratio}]
          // node_indices = nodeId -> index in nodes

          const compute_link_offset = (at) => node_indices[at] * node_distance;
          for (const link of links) {
            link.start_pos = compute_link_offset(link.source);
            link.end_pos = compute_link_offset(link.target);
          }
          // the links now have (sourceId, targetId, frequency, start_pos, end_pos)
          // thus, everything required to draw them

          console.log(node_weights);
          console.log(links);

          // links: [(sourceId, targetId, frequency, start_pos, end_pos)]
          // raw_nodes: [nodeId]
          // nodes: [{nodeId, pos, weight, weight_ratio}]
          return {
            links,
            raw_nodes,
            nodes,
          };
        }

        get_svg(elementId) {
          const element = document.getElementById(elementId);
          const parent = element.parentElement;
          this.settings.radius = Math.min(
              parent.clientWidth / 2,
              parent.parentElement.parentElement.clientHeight / 2 - 30,
          ) - this.settings.padding;

          this.settings.radius = Math.max(this.settings.radius, this.settings.min_radius);

          this.settings.node_count = Math.floor(this.settings.radius / 2);

          const chart = d3.select(element);

          chart.selectAll('svg').remove();

          const svg = chart
              .append('svg')
              .style('margin-left', 'auto')
              .style('margin-right', 'auto')
              .attr('width', 2 * this.settings.radius + 2 * this.settings.padding)
              .attr('height', 2 * this.settings.radius + 2 * this.settings.padding);

          const offset = this.settings.padding + this.settings.radius;

          return svg.append('g').attr('transform', 'translate(' + offset + ',' + offset + ')');
        }

       draw(elementId) {
          const svg = this.get_svg(elementId);

          // let data = prepare_data(raw_data);

          let chartData = data.getData('relationCounts');
          const nodesById = new Map();
          chartData.nodes.forEach((n) => nodesById.set(n.id, n));
          console.log('Relation counts for circle chart:');
          console.log(chartData);
          chartData = this.prepare_data(chartData.links);

          console.log('Circle chart preprocessed data:');
          console.log(chartData);


          this.nodes = svg.append('g')
              .attr('class', 'nodes')
              .selectAll('g.node')
              .data(chartData.nodes)
              .enter().append('g')
              .attr('class', 'node')
              .attr('opacity', this.hoverless_node_opacity())
              .attr('transform', function(d) {
                return 'rotate(' + (d.pos - 90) + ')';
              });


          let theNodeHover = this.nodeHover;
          let theNodeUnhover = this.nodeUnhover;
          theNodeHover = theNodeHover.bind(this);
          theNodeUnhover = theNodeUnhover.bind(this);
          const node_texts = this.nodes.append('text')
              .attr('class', 'nodeText')
              .attr('x', this.settings.radius + 10)
              .attr('y', 4)
              .text((d) => nodesById.get(d.id).name)
              .property('onmouseover', () => theNodeHover)
              .property('onmouseout', () => theNodeUnhover)
              .attr('fill', (d) => {
                const red = Math.round(d.weight_ratio * 100);
                const green = Math.round((1 - d.weight_ratio) * 100);

                return 'rgb('+red+','+green+',0)';
              });

          node_texts.filter((d) => ((d.pos % 360) + 360) % 360 > 180)
              .attr('transform', 'rotate(180)')
              .attr('x', -this.settings.radius - 10)
              .attr('text-anchor', 'end');


          // hack: global
          if (window.opacity_base === undefined) {
            window.opacity_base = 0.99;
          }

          this.links = svg.append('g')
              .attr('class', 'links')
              .selectAll('path.link')
              .data(chartData.links)
              .enter().append('path')
              .attr('class', 'link')
              .attr('fill', 'none')
              .attr('opacity', (d) => (1 - Math.pow(window.opacity_base, d.frequency)))
              .attr('stroke', this.hoverless_link_color())
              .attr('d', link => this.computeLinkPath(link))
              .attr('stroke-width', 5);

          let opacity_redraw = () => draw(this.elementId);
        }

        nodeHover(event) {
          this.hovered_id = event.target.__data__.id;

          const connected_nodes = {};
          connected_nodes[this.hovered_id] = 10000000;

          this.links.attr('stroke', this.settings.inactive_link_color);

          this.links.filter((link) => {
            if (link.source === this.hovered_id) {
              connected_nodes[link.target] = link.frequency;
              return true;
            } else if (link.target === this.hovered_id) {
              connected_nodes[link.source] = link.frequency;
              return true;
            } else {
              return false;
            }
          }).attr('stroke', this.settings.active_link_color).raise();

          this.nodes.attr('opacity', (n) => {
            if (this.settings.fine_node_highlights) {
              const v1 = 1 - Math.pow(0.97, connected_nodes[n.id] || 0);
              return (this.settings.active_node_opacity - this.settings.inactive_node_opacity) * v1 +
                        this.settings.inactive_node_opacity;
            } else {
              if (connected_nodes[n.id]) {
                return this.settings.active_node_opacity;
              } else {
                return this.settings.inactive_node_opacity;
              }
            }
          });
        }

        nodeUnhover(event) {
          const unhovered_id = event.target.__data__.id;

          if (unhovered_id === this.hovered_id) {
            this.hovered_id = '';
            this.links.attr('stroke', this.hoverless_link_color());
            this.nodes.attr('opacity', this.hoverless_node_opacity());
          }
        }

        hoverless_link_color() {
          if (this.settings.default_grey) {
            return this.settings.inactive_link_color;
          } else {
            return this.settings.active_link_color;
          }
        }

        hoverless_node_opacity() {
          if (this.settings.default_grey) {
            return this.settings.inactive_node_opacity;
          } else {
            return this.settings.active_node_opacity;
          }
        }

        degToCoord(deg) {
          const r = this.settings.radius;
          const rad = deg / 180 * Math.PI;
          return (r * Math.sin(rad)) + ' ' + (-1 * r * Math.cos(rad));
        }

        computeLinkPath(link) {
          const {
            start_pos,
            end_pos,
          } = link;
          const path = 'M ' + this.degToCoord(start_pos) +
                ' Q 0 0 ' + this.degToCoord(end_pos);

          return path;
        }

        add_toggle(elementId, id, text, initial_state, change_handler) {
          const p = d3.select('#'+elementId+'-container .settings .checkboxes').append('p');
          console.log(p.node());
          const input = p.append('input').attr('type', 'checkbox').attr('id', id);
          if (initial_state) {
            input.attr('checked', 'checked');
          }
          p.append('label').attr('for', id).text(' '+text);
          input.on('change', function() {
            change_handler(this.checked);
            draw(elementId);
          });
        }

        add_slider(elementId, id, label_text, min, max, step, value, change_handler) {
          const p = d3.select('#' + elementId + '-container .settings .sliders').append('p');

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
                draw(this.elementId);
              });
        }
  }

  return function newCircleChartWidget(elementId, widgetSettings) {
    widgetManager.addWidget(widgetSettings.widgetId, new CircleChartWidget(elementId, widgetSettings));
  };
});
