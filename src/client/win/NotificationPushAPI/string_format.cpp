#include "stdafx.h"
#include "string_format.h"

std::string string_format(const std::string fmt_str, ...) {
    int size = 512;
    char* buffer = new char[size+1];
    va_list vl;
	va_start(vl, fmt_str);
    int ret = vsnprintf_s(buffer, size+1, size, fmt_str.c_str(), vl);
    while (ret == -1){ //fail, delete buffer and try again
        delete[] buffer;
		size *= 2;
        buffer = new char[size+1];
        ret = vsnprintf_s(buffer, size+1, size, fmt_str.c_str(), vl);
    }
    std::string result(buffer);
    va_end(vl);
    delete[] buffer;
    return result;
}
