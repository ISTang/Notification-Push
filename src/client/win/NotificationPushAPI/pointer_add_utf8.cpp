#include "stdafx.h"
#include "pointer_add_utf8.h"

const char * pointer_add_utf8(const char *s, int n)
{
	if (n == 0) return s;

	const char *p = s;
	int i = 0, j = 0;
	while (*p)
	{
		if ((*p & 0xc0) != 0x80)
		{
			j++;
			if (j == n) break;
		}
		p++;
	}
	return p;
}
