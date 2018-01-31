#!/usr/bin/env python3
"""
A script for creating scripts for submitting to Slurm <https://slurm.schedmd.com/> to run cross-validation tests.

See https://slurm.schedmd.com/sbatch.html
"""

__author__ = "Todd Shore <errantlinguist+github@gmail.com>"
__copyright__ = "Copyright 2017 Todd Shore"
__license__ = "Apache License, Version 2.0"

import argparse
import datetime
import posixpath
import sys
from enum import Enum, unique
from typing import Callable

__DEFAULT_ACCOUNT = "2018-1"
__DEFAULT_TIME = "8:00:00"
__DEFAULT_HEAPSIZE = "10g"


def _create_default_project_dirpath(user: str) -> str:
	return "/cfs/klemming/nobackup/{first_initial}/{user}/tangrams-restricted".format(first_initial=user[0],
																					  user=user)


def __create_default_job_name(current_time: datetime.datetime) -> str:
	return "tangrams-" + str(current_time.timestamp())


def __create_argparser(current_time: datetime.datetime) -> argparse.ArgumentParser:
	default_job_name = __create_default_job_name(current_time)
	result = argparse.ArgumentParser(
		description="Creates a script for submitting to Slurm to run cross-validation tests.")
	result.add_argument("user", metavar="USER",
						help="The system username to use for running the batch script.")
	result.add_argument("-j", "--job-name", dest="job_name", metavar="JOBNAME", default=default_job_name,
						help="Specify a name for the job allocation. The specified name will appear along with the job id number when querying running jobs on the system.")
	result.add_argument("-a", "--account", metavar="ACCOUNT", default=__DEFAULT_ACCOUNT,
						help="Charge resources used by this job to specified account.")
	result.add_argument("-t", "--time", metavar="TIME", default=__DEFAULT_TIME,
						help="Set a limit on the total run time of the job allocation.")
	result.add_argument("-p", "--heap", metavar="HEAPSIZE", default=__DEFAULT_HEAPSIZE,
						help="Set a limit on the total run time of the job allocation.")
	result.add_argument("-d", "--project-dir", dest="project_dir", metavar="DIRPATH",
						help="The project root directory.")
	result.add_argument("-i", "--indir", default="Data",
						metavar="DIRPATH",
						help="The path of the directory containing the session data to use for cross-validation relative to the project root.")
	result.add_argument("-o", "--outdir", default="output",
						metavar="DIRPATH",
						help="The path of the directory to write the cross-validation results to relative to the project root.")
	result.add_argument("-r", "--reflang", default="Data/utt-referring-tokens-lemma.tsv",
						metavar="FILENAME",
						help="The path of the referring-language file to read relative to the project root.")
	result.add_argument("-m", "--model-params", default="Data/model-params.tsv",
						dest="model_params",
						metavar="FILENAME",
						help="The path of the model-parameters file to read relative to the project root.")
	result.add_argument("-c", "--classpath-jar",
						dest="classpath_jar", default="tangrams-wac-0.0.1-SNAPSHOT-jar-with-dependencies.jar",
						metavar="PATH",
						help="The path of the JAR file to run relative to the project root.")
	return result


SCRIPT_FORMAT_STR = """#!/bin/bash -l
# The -l above is required to get the full environment with modules

# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
# 
# 	http://www.apache.org/licenses/LICENSE-2.0
# 
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.


# A generated script for running cross-validation using Slurm <https://slurm.schedmd.com/>.
#
# This file was generated on {creation_date}.
# 
# Author: Todd Shore <errantlinguist+github@gmail.com>
# Since: 2017-12-05

# Set the allocation to be charged for this job
# not required if you have set a default allocation
#SBATCH -A {account}

# The name of the batch job. NOTE: This will properly appear in e.g. email notifications even though the name is truncated in output of "squeue"
#SBATCH -J {job_name}

# Wall-clock time that will be given to this job
#SBATCH -t {time}

#SBATCH -o "{output_dir}/{job_name}-sbatch.out.txt"
#SBATCH -e "{output_dir}/{job_name}-sbatch.err.txt"

#SBATCH --mail-user={user}
#SBATCH --mail-type=ALL

module add jdk/1.8.0_45

CLASSPATH_JARFILE="{classpath_jar}"
echo "JAR file is \\"${{CLASSPATH_JARFILE}}\\"."
INPATH="{input_dir}"
echo "Inpath is \\"${{INPATH}}\\"."
REFLANG_FILE="{reflang_file}"
echo "Referring-language file is \\"${{REFLANG_FILE}}\\"."
PARAMS_FILE="{params_file}"
echo "Model-parameters file is \\"${{PARAMS_FILE}}\\"."
OUTDIR="{output_dir}/{job_name}"
echo "Outdir is \\"${{OUTDIR}}\\"."

STD_OUTFILE="{output_dir}/{job_name}.out.txt"
echo "Standard output stream is directed to \\"${{STD_OUTFILE}}\\"."
ERR_OUTFILE="{output_dir}/{job_name}.err.txt"
echo "Error output stream is directed to \\"${{ERR_OUTFILE}}\\"."

HEAP_SIZE="{heap_size}"
echo "Will use heap space size of ${{HEAP_SIZE}}."

java -server -Xmx${{HEAP_SIZE}} -jar "${{CLASSPATH_JARFILE}}" "${{INPATH}}" -t "${{REFLANG_FILE}}" -p "${{PARAMS_FILE}}" -o "${{OUTDIR}}" > "${{STD_OUTFILE}}" 2> "${{ERR_OUTFILE}}"
"""


def __main(args, current_time: datetime.datetime):
	job_name = args.job_name
	print("Job name: {}".format(job_name), file=sys.stderr)
	account = args.account
	print("Account: {}".format(account), file=sys.stderr)
	time = args.time
	print("Time: {}".format(time), file=sys.stderr)
	user = args.user
	print("User: {}".format(user), file=sys.stderr)
	user_email = user + "@kth.se"
	print("Notification e-mail address: {}".format(user_email), file=sys.stderr)

	project_dir = args.project_dir
	if project_dir is None:
		project_dir = _create_default_project_dirpath(user)
	print("Project directory root: {}".format(project_dir), file=sys.stderr)

	indir = posixpath.join(project_dir, args.indir)
	print("Input directory: {}".format(indir), file=sys.stderr)
	outdir = posixpath.join(project_dir, args.outdir)
	print("Output directory: {}".format(outdir), file=sys.stderr)
	classpath_jar = posixpath.join(project_dir, args.classpath_jar)
	print("Classpath JAR: {}".format(classpath_jar), file=sys.stderr)
	reflang_filepath = posixpath.join(project_dir, args.reflang)
	print("Referring-language file path: {}".format(reflang_filepath), file=sys.stderr)
	model_params_filepath = posixpath.join(project_dir, args.model_params)
	print("Model-parameters file path: {}".format(model_params_filepath), file=sys.stderr)
	heap_size = args.heap
	print("Heap size: {}".format(heap_size), file=sys.stderr)

	creation_timestamp = current_time.isoformat()
	script_str = SCRIPT_FORMAT_STR.format(creation_date=creation_timestamp, job_name=job_name, account=account,
										  time=time, user=user_email, input_dir=indir,
										  output_dir=outdir, classpath_jar=classpath_jar, reflang_file=reflang_filepath,
										  params_file=model_params_filepath, heap_size=heap_size)
	print(script_str)


if __name__ == "__main__":
	__current_time = datetime.datetime.utcnow()
	__main(__create_argparser(__current_time).parse_args(), __current_time)
