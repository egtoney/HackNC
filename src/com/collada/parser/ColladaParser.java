package com.collada.parser;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ColladaParser {

	public static Model parseModel(Document doc) {
		NodeList library_nodes = doc.getChildNodes().item(0).getChildNodes();
		HashMap<String, Integer> node_map = new HashMap<>();
		Model output = new Model();
		
		// Map node indexes to associated names
		for( int i=0 ; i<library_nodes.getLength() ; i++ ){
			Node curr_node = library_nodes.item(i);
			String curr_node_name = curr_node.getNodeName();
			if( curr_node_name.equals("library_lights") ){
				node_map.put("library_lights", i);
			}else if( curr_node_name.equals("library_images") ){
				node_map.put("library_images", i);
			}else if( curr_node_name.equals("library_effects") ){
				node_map.put("library_effects", i);
			}else if( curr_node_name.equals("library_materials") ){
				node_map.put("library_materials", i);
			}else if( curr_node_name.equals("library_geometries") ){
				node_map.put("library_geometries", i);
			}else if( curr_node_name.equals("library_cameras") ){
				node_map.put("library_cameras", i);
			}else if( curr_node_name.equals("library_controllers") ){
				node_map.put("library_controllers", i);
			}else if( curr_node_name.equals("library_visual_scenes") ){
				node_map.put("library_visual_scenes", i);
			}else if( curr_node_name.equals("scene") ){
				node_map.put("scene", i);
			}
		}
		
		// Load in the geometry data
		if( node_map.containsKey("library_geometries") ){
			output = loadGeometries( output, library_nodes.item(node_map.get("library_geometries")) );
		}
		
		return output;
	}
	
	private static Model loadGeometries( Model model, Node library_geometries ){
		// Load all geometries
		NodeList geometries = library_geometries.getChildNodes();
		for( int i=0 ; i<geometries.getLength() ; i++ ){
			if( geometries.item(i).getNodeName().equals("geometry") ){
				
				// Load all meshes
				NodeList meshes = geometries.item(i).getChildNodes();
				for( int j=0 ; j<meshes.getLength() ; j++ ){
					if( meshes.item(j).getNodeName().equals("mesh") ){
						
						NodeList parts = meshes.item(j).getChildNodes();
						HashMap<String, Integer> node_map = new HashMap<>();
						
						// Map all parts of the mesh
						for( int k=0 ; k<parts.getLength() ; k++ ){
							Node curr_part = parts.item(k);
							String tag_name = curr_part.getNodeName();
							
							if( tag_name.equals("polylist") ){
								node_map.put("polylist", k);

							// Check to make sure that it has attributes
							}else if( curr_part.getNodeType() == Node.ELEMENT_NODE ){
								Element curr_element = (Element) curr_part;
								
								if( curr_element.hasAttribute("id") ){
									String element_id = curr_element.getAttribute("id");
									
									// Load in sources
									if( element_id.contains("positions") ){
										node_map.put("positions", k);
									
									}else if( element_id.contains("normals") ){
										node_map.put("normals", k);
										
									}
								}
							// Load in polygon list
							} 
						}
		
						ArrayList<Vertex> verts = loadVerts( parts.item( node_map.get("positions") ) );
						ArrayList<Vertex> normals = loadVerts( parts.item( node_map.get("normals") ) );
						ArrayList<Vertex[]> polys = loadPolygons( parts.item( node_map.get("polylist") ), verts, normals );
						
						model.addVertecies(verts);
						model.addNormals(normals);
						model.addPolygons(polys);
						
						return model;
					}
				}
			}
		}
		return model;
	}
	
	private static ArrayList<Vertex[]> loadPolygons( Node node, ArrayList<Vertex> verts, ArrayList<Vertex> normals ) {
		ArrayList<Vertex[]> out_polys = new ArrayList<>();
		HashMap<String, Integer> node_map = new HashMap<>();
		NodeList nodes = node.getChildNodes();
		
		for( int i=0 ; i<nodes.getLength() ; i++ ){
			String node_name = nodes.item(i).getNodeName();
			if( node_name.equals("vcount") ){
				node_map.put("vcount", i);
			}else if( node_name.equals("p") ){
				node_map.put("p", i);
			}
		}
		
		Node vcount = nodes.item(node_map.get("vcount"));
		String vcounts = vcount.getTextContent();
		String[] vert_count = vcounts.trim().split(" ");
		
		Node p = nodes.item(node_map.get("p"));
		String polys = p.getTextContent();
		String[] poly_pts = polys.trim().split(" ");
		
		for( int i=0, k=0, w=0 ; i<poly_pts.length ; i+=2*k, w++ ){
			k = Integer.parseInt(vert_count[w]);
			Vertex[] polygon = new Vertex[k];

			int p1 = Integer.parseInt(poly_pts[i]);
			int p2 = Integer.parseInt(poly_pts[i+2]);
			int p3 = Integer.parseInt(poly_pts[i+4]);
			
			polygon[0] = verts.get(p1);
			polygon[1] = verts.get(p2);
			polygon[2] = verts.get(p3);
			
			out_polys.add(polygon);
		}
		return out_polys;
	}

	private static ArrayList<Vertex> loadVerts( Node node ){
		ArrayList<Vertex> vertecies = new ArrayList<>();
		NodeList nodes = node.getChildNodes();
		for( int i=0 ; i<nodes.getLength() ; i++ ){
			if( nodes.item(i).getNodeName().equals("float_array") ){
				String vert_vals = nodes.item(i).getTextContent();
				String[] vals = vert_vals.split(" ");
				for( int q=0 ; q<vals.length ; q+=3){
					float nx = Float.parseFloat(vals[q]);
					float ny = Float.parseFloat(vals[q+1]);
					float nz = Float.parseFloat(vals[q+2]);
					Vertex nv = new Vertex(nx, ny, nz);
					vertecies.add(nv);
				}
			}
		}
		return vertecies;
	}
	
}
