/^#|^$/ { next }
{
	print "Number of items = "NF;
	for (i=1; i<NF+1; i++) {
		print $i
	}
}