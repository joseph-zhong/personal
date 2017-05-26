
#include <cstdio>
#include <cstdlib>
#include <fstream>
#include <iostream>
#include <string>
#include <iterator>

const std::string hw("Hello World\n");

inline void checkErr(cl_int err, const char * name) {
if (err != CL_SUCCESS) {
std::cerr << "ERROR: " << name  << " (" << err << ")" << std::endl;
exit(EXIT_FAILURE);
}
}


int main(void) {
	cl_int err;
	cl::vector< cl::Platform > platformList;
	cl::Platform::get(&platformList);
	checkErr(platformList.size()!=0 ? CL_SUCCESS : -1, "cl::Platform::get");

	std::cerr << "Platform number is: " << platformList.size() << std::endl;std::string platformVendor;

	platformList[0].getInfo((cl_platform_info)CL_PLATFORM_VENDOR, &platformVendor);
	std::cerr << "Platform is by: " << platformVendor << "\n";

	cl_context_properties cprops[3] = {
		CL_CONTEXT_PLATFORM, 
		(cl_context_properties) (platformList[0])(), 0
	};

	cl::Context context(	CL_DEVICE_TYPE_CPU,	cprops,	NULL,	NULL,	&err);
	checkErr(err, "Context::Context()"); 
	
	char * outH = new char[hw.length()+1];
	cl::Buffer outCL(context,	CL_MEM_WRITE_ONLY | CL_MEM_USE_HOST_PTR,	hw.length()+1,	outH,	&err);
	checkErr(err, "Buffer::Buffer()");

}

