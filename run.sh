
if [ $# -ne 3 ]
then
    echo "$0 [start_date] [end_date] [limit_area]"
    exit
fi

start_date=$1
end_date=$2
limit_area=$3

hope package delivery_area.hope
if [ $? -ne 0 ]
then
    echo "compile failure, please re-compiling"
    exit
fi
hope run delivery_scope.hope --args="$start_date $end_date $limit_area"