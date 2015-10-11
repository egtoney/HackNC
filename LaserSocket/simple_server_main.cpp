#include "ServerSocket.h"
#include "SocketException.h"
#include <string>
#include <iostream>
#include <sstream>
#include <vector>
#include "LaserDriver.h"

using namespace std;

bool hasEnding( string const &fullString, string const &ending ){
	if( fullString.length() >= ending.length() ){
		return (0 == fullString.compare( fullString.length() - ending.length(), ending.length(), ending));
	}else{
		return false;
	}
}

int main ( int argc, int argv[] )
{

  LaserDriver LD;
  std::cout << "running....\n";

  try
    {
      // Create the socket
      ServerSocket server ( 30000 );

      vector< vector< int > > points;

      while ( true )
	{

	  ServerSocket new_sock;
	  server.accept ( new_sock );
         	 server.set_non_blocking( false );

	string buffer = "";

	  try
	    {
	      while ( true ){
	      	points.erase( points.begin(), points.begin()+points.size());
	                  cout << "here1" << endl;
	                  std::string line = "";
	                  //std::string buffer="";
		  int x, y;
		while( true ){
		  new_sock >> buffer;
		  stringstream bss;
		  bss << buffer;

		  int sentinel = 0;
		  int prev_numb = 0;
		  int number = 0;
		  bool first = true;
		  bool done = false;
		  while( bss >> sentinel ){
			bss >> number;
		  	if (sentinel < 0 || number < 0){
				done = true;
		  		break;
		  	}

		  	else{
		  		vector<int> pts;
		  		pts.push_back(sentinel);
		  		pts.push_back(number);
		  		points.push_back(pts);
		  	}
		  }
		  if( done )
			break;
		}



	                  // if( line.length() > 0 ){
	                  //     cout << "reading..." << endl;
	                  //     points.erase( points.begin(), points.begin()+points.size() );
	                  //     std::stringstream ss;
	                  //     ss << line;
	                  //     while(ss >> y >> x){
	                  //          std::vector<int> pt;
	                  //          pt.push_back(x);
	                  //          pt.push_back(y);
	                  //          points.push_back(pt);
	                           // std::cout << x << " " << y << std::endl;
			           // LD.lSet(x,y);
	                  //     }
	                  //     cout << "done..." << endl;
	                  // }
	                  //  cout << "here2" << endl;

	                  for( int i=0 ; i<points.size() ; i++ ){
	                      cout << points.size() << endl;
	                      LD.lSet(points[i][0],points[i][1]);
	                      //delayMicroseconds(500);
	                  }
			  // std::cout << data << std::endl;
			  // new_sock << data;
		}
	    }
	  catch ( SocketException& ) {}

	}
    }
  catch ( SocketException& e )
    {
      std::cout << "Exception was caught:" << e.description() << "\nExiting.\n";
    }

  return 0;
}
