#include "stdafx.h"
#include "strlen_utf8.h"

int strlen_utf8(const char *s)
{
	int i = 0, j = 0;
	while (s[i])
	{
		if ((s[i] & 0xc0) != 0x80) j++;
		i++;
	}
	return j;
}
