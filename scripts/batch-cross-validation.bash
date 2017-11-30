#!/bin/bash -l
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


# A batch script for running cross-validation using Slurm <https://slurm.schedmd.com/>.
# 
# Author: Todd Shore <tcshore@kth.se>
# Since: 2017-11-28

# Set the allocation to be charged for this job
# not required if you have set a default allocation
#SBATCH -A 2017-130

# The name of the script
#SBATCH -J tangrams-wac

# Wall-clock time that will be given to this job
#SBATCH -t 5:00:00

#SBATCH -e "/cfs/klemming/nobackup/t/tcshore/tangrams-restricted/output/sbatch.err.txt"
#SBATCH -o "/cfs/klemming/nobackup/t/tcshore/tangrams-restricted/output/sbatch.out.txt"

#SBATCH --mail-user=tcshore@kth.se
#SBATCH --mail-type=ALL

module add jdk/1.8.0_45

PROJECT_DIR="/cfs/klemming/nobackup/${LOGNAME:0:1}/${LOGNAME}/tangrams-restricted"
#PROJECT_DIR="${HOME}/Private/tangrams-restricted"

CLASSPATH_JARFILE="${PROJECT_DIR}/tangrams-wac-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
echo "JAR file is \"${CLASSPATH_JARFILE}\"."
INPATH="${PROJECT_DIR}/Data/Ready"
echo "Inpath is \"${INPATH}\"."
REFLANG_FILE="${PROJECT_DIR}/Data/utt-referring-tokens-lemma.tsv"
echo "Referring-language file is \"${REFLANG_FILE}\"."
PARAMS_FILE="${PROJECT_DIR}/Data/model-params.tsv"
echo "Model parameter file is \"${PARAMS_FILE}\"."
OUTDIR="${PROJECT_DIR}/output/tangrams-wac"
echo "Outdir is \"${OUTDIR}\"."

STD_OUTFILE="${PROJECT_DIR}/output/tangrams-wac.out.txt"
echo "Standard output stream is directed to \"${STD_OUTFILE}\"."
ERR_OUTFILE="${PROJECT_DIR}/output/tangrams-wac.err.txt"
echo "Error output stream is directed to \"${ERR_OUTFILE}\"."

HEAP_SIZE="10240m"
echo "Will use heap space size of ${HEAP_SIZE}."

java -server -Xmx${HEAP_SIZE} -jar "${CLASSPATH_JARFILE}" "${INPATH}" -t "${REFLANG_FILE}" -p "${PARAMS_FILE}" -o "${OUTDIR}" > "${STD_OUTFILE}" 2> "${ERR_OUTFILE}"