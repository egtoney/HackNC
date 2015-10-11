package com.collada.parser;

import java.util.ArrayList;

public class Model {

	private static final ArrayList<Vertex> vertecies = new ArrayList<>();
	private static final ArrayList<Vertex> normals = new ArrayList<>();
	private static final ArrayList<Vertex[]> polygons = new ArrayList<>();
	
	public Model(){
		
	}
	
	public Model(ArrayList<Vertex> verts, ArrayList<Vertex> normals2, ArrayList<Vertex[]> polys) {
		vertecies.addAll(verts);
		normals.addAll(normals2);
		polygons.addAll(polys);
	}

	public void addVertecies( ArrayList<Vertex> new_verts ){
		vertecies.addAll(new_verts);
	}
	
	public void addNormals( ArrayList<Vertex> new_norms ){
		normals.addAll(new_norms);
	}

	public void addPolygons(ArrayList<Vertex[]> polys) {
		polygons.addAll(polys);
	}
	
	@Override
	public String toString(){
		return "{verts:"+ vertecies.size() +", normals:"+ normals.size() +", polygons:"+ polygons.size() +"}";
	}
	
}
