#! /bin/sh ~
if [ $# -ne 1 ]
	then echo "usage check_CN.sh <dir>"
	exit
fi
{
	function check(){
		for file in `ls $1`
		do
			if [ -d $1"/"$file ]
			then
				check $1"/"$file
			else
				local path=$1"/"$file
				local name=$file
				if [ "${name##*.}" != "java" ]
					then continue
				fi
				if [[ `cat $path | sed 's/[a-zA-Z0-9[:punct:]ø]//g' | grep -v '^[[:space:]]*$'` ]]
					then echo ""
						echo $path
				fi
				cat -n $path | sed 's/[a-zA-Z0-9[:punct:]ø]//g' | grep -v '^[[:space:]]*$'
			fi
		done
	}
}
check $1
